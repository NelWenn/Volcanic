package net.vulkanmod.vulkan;

import net.vulkanmod.Initializer;
import net.vulkanmod.config.Platform;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libffi.FFICIF;
import org.lwjgl.system.macosx.ObjCRuntime;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.libffi.LibFFI.*;

import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTMetalSurface.*;
import static org.lwjgl.vulkan.KHRSurface.VK_KHR_SURFACE_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRWaylandSurface.*;
import static org.lwjgl.vulkan.KHRWin32Surface.*;
import static org.lwjgl.vulkan.KHRXlibSurface.*;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

// Boot-safe replacements for the LWJGL GLFWVulkan helpers: on NeoForge GLFWVulkan lives in the BOOT
// layer and references org.lwjgl.vulkan.VK, which our GAME-layer lwjgl-vulkan can't be read from, so
// touching it throws NoClassDefFoundError. Build the extensions and surface ourselves instead. The
// GLFWNative* handle getters have no vulkan references, so they're safe.
public final class VkSurfaceUtil {
    private VkSurfaceUtil() {}

    // Replacement for glfwGetRequiredInstanceExtensions().
    public static PointerBuffer getRequiredInstanceExtensions(MemoryStack stack, boolean withDebugUtils) {
        String platformSurface;
        if (Platform.isWindows())      platformSurface = VK_KHR_WIN32_SURFACE_EXTENSION_NAME;
        else if (Platform.isMacOS())   platformSurface = VK_EXT_METAL_SURFACE_EXTENSION_NAME;
        else if (Platform.isWayLand()) platformSurface = VK_KHR_WAYLAND_SURFACE_EXTENSION_NAME;
        else                           platformSurface = VK_KHR_XLIB_SURFACE_EXTENSION_NAME; // X11 (or unknown)

        // via the loader (validation mode) MoltenVK is hidden unless portability enumeration is enabled
        boolean portability = net.vulkanmod.vulkan.MoltenVKConfig.validationEnabled();

        int count = 2 + (withDebugUtils ? 1 : 0) + (portability ? 1 : 0);
        PointerBuffer extensions = stack.mallocPointer(count);
        extensions.put(stack.UTF8(VK_KHR_SURFACE_EXTENSION_NAME));
        extensions.put(stack.UTF8(platformSurface));
        if (withDebugUtils) {
            extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
        }
        if (portability) {
            extensions.put(stack.UTF8(org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME));
        }
        return extensions.rewind();
    }

    // Replacement for glfwCreateWindowSurface().
    public static long createSurface(VkInstance instance, long window, MemoryStack stack) {
        if (Platform.isMacOS())        return createMetalSurface(instance, window, stack);
        else if (Platform.isWindows()) return createWin32Surface(instance, window, stack);
        else if (Platform.isWayLand()) return createWaylandSurface(instance, window, stack);
        else                           return createXlibSurface(instance, window, stack);
    }

    // ---- macOS / MoltenVK -----------------------------------------------------------------

    private static long createMetalSurface(VkInstance instance, long window, MemoryStack stack) {
        long nsWindow = org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaWindow(window);
        if (nsWindow == 0L) throw new RuntimeException("glfwGetCocoaWindow returned NULL");

        // attach a CAMetalLayer to the contentView (objc_msgSend directly; no LWJGL wrapper)
        long objc_msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
        long contentView = JNI.invokePPP(nsWindow, ObjCRuntime.sel_getUid("contentView"), objc_msgSend);
        long metalLayer  = JNI.invokePPP(ObjCRuntime.objc_getClass("CAMetalLayer"), ObjCRuntime.sel_getUid("layer"), objc_msgSend);
        if (contentView == 0L || metalLayer == 0L) throw new RuntimeException("Failed to obtain contentView/CAMetalLayer");
        JNI.invokePPPP(contentView, ObjCRuntime.sel_getUid("setWantsLayer:"), 1L, objc_msgSend); // setWantsLayer:YES
        JNI.invokePPPP(contentView, ObjCRuntime.sel_getUid("setLayer:"), metalLayer, objc_msgSend);

        // contentsScale must match the framebuffer/window ratio so the drawable size equals the swapchain size
        int[] fbW = { 0 }, fbH = { 0 }, winW = { 0 }, winH = { 0 };
        org.lwjgl.glfw.GLFW.glfwGetFramebufferSize(window, fbW, fbH);
        org.lwjgl.glfw.GLFW.glfwGetWindowSize(window, winW, winH);
        double scale = (winW[0] > 0) ? ((double) fbW[0] / (double) winW[0]) : 1.0;
        if (scale <= 0.0) scale = 1.0;
        // HiDPI disabled: force a 1x drawable (logical resolution, not the 2x Retina backing store)
        if (net.vulkanmod.config.Platform.isMacOS() && net.vulkanmod.Initializer.CONFIG.disableHiDPI) {
            scale = 1.0;
        }
        Initializer.LOGGER.info("VulkanMod: Metal drawable scale = {} (framebuffer {}x{} / window {}x{})",
                scale, fbW[0], fbH[0], winW[0], winH[0]);
        setContentsScale(objc_msgSend, metalLayer, scale, stack);

        VkMetalSurfaceCreateInfoEXT createInfo = VkMetalSurfaceCreateInfoEXT.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_METAL_SURFACE_CREATE_INFO_EXT);
        // write the CAMetalLayer id straight into pLayer
        org.lwjgl.system.MemoryUtil.memPutAddress(createInfo.address() + VkMetalSurfaceCreateInfoEXT.PLAYER, metalLayer);

        LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);
        int r = vkCreateMetalSurfaceEXT(instance, createInfo, null, pSurface);
        Vulkan.checkResult(r, "Failed to create Metal surface");
        Initializer.LOGGER.info("VulkanMod: created VkSurfaceKHR via VK_EXT_metal_surface (MoltenVK)");
        return pSurface.get(0);
    }

    // ---- Windows --------------------------------------------------------------------------

    private static long createWin32Surface(VkInstance instance, long window, MemoryStack stack) {
        long hwnd = org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(window);
        // HINSTANCE from the window (GWLP_HINSTANCE = -6)
        long hinstance = org.lwjgl.system.windows.User32.GetWindowLongPtr(hwnd, -6);

        VkWin32SurfaceCreateInfoKHR createInfo = VkWin32SurfaceCreateInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_WIN32_SURFACE_CREATE_INFO_KHR)
                .hinstance(hinstance)
                .hwnd(hwnd);

        LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);
        int r = vkCreateWin32SurfaceKHR(instance, createInfo, null, pSurface);
        Vulkan.checkResult(r, "Failed to create Win32 surface");
        Initializer.LOGGER.info("VulkanMod: created VkSurfaceKHR via VK_KHR_win32_surface");
        return pSurface.get(0);
    }

    // ---- Linux X11 ------------------------------------------------------------------------

    private static long createXlibSurface(VkInstance instance, long window, MemoryStack stack) {
        long display = org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Display();
        long x11Window = org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Window(window);

        VkXlibSurfaceCreateInfoKHR createInfo = VkXlibSurfaceCreateInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_XLIB_SURFACE_CREATE_INFO_KHR)
                .dpy(display)
                .window(x11Window);

        LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);
        int r = vkCreateXlibSurfaceKHR(instance, createInfo, null, pSurface);
        Vulkan.checkResult(r, "Failed to create Xlib surface");
        Initializer.LOGGER.info("VulkanMod: created VkSurfaceKHR via VK_KHR_xlib_surface");
        return pSurface.get(0);
    }

    // ---- Linux Wayland --------------------------------------------------------------------

    private static long createWaylandSurface(VkInstance instance, long window, MemoryStack stack) {
        long display = org.lwjgl.glfw.GLFWNativeWayland.glfwGetWaylandDisplay();
        long waylandSurface = org.lwjgl.glfw.GLFWNativeWayland.glfwGetWaylandWindow(window);

        VkWaylandSurfaceCreateInfoKHR createInfo = VkWaylandSurfaceCreateInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_WAYLAND_SURFACE_CREATE_INFO_KHR)
                .display(display)
                .surface(waylandSurface);

        LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);
        int r = vkCreateWaylandSurfaceKHR(instance, createInfo, null, pSurface);
        Vulkan.checkResult(r, "Failed to create Wayland surface");
        Initializer.LOGGER.info("VulkanMod: created VkSurfaceKHR via VK_KHR_wayland_surface");
        return pSurface.get(0);
    }

    // [metalLayer setContentsScale:scale] via libffi (LWJGL has no objc_msgSend trampoline taking a double).
    private static void setContentsScale(long objc_msgSend, long metalLayer, double scale, MemoryStack stack) {
        long sel = ObjCRuntime.sel_getUid("setContentsScale:");
        FFICIF cif = FFICIF.malloc(stack);
        PointerBuffer argTypes = stack.mallocPointer(3);
        argTypes.put(0, ffi_type_pointer);
        argTypes.put(1, ffi_type_pointer);
        argTypes.put(2, ffi_type_double);
        if (ffi_prep_cif(cif, FFI_DEFAULT_ABI, ffi_type_void, argTypes) != FFI_OK) {
            Initializer.LOGGER.warn("VulkanMod: ffi_prep_cif failed; Retina contentsScale not applied");
            return;
        }
        PointerBuffer receiver = stack.pointers(metalLayer);
        PointerBuffer selector = stack.pointers(sel);
        DoubleBuffer scaleValue = stack.doubles(scale);
        PointerBuffer argValues = stack.mallocPointer(3);
        argValues.put(0, MemoryUtil.memAddress(receiver));
        argValues.put(1, MemoryUtil.memAddress(selector));
        argValues.put(2, MemoryUtil.memAddress(scaleValue));
        ffi_call(cif, objc_msgSend, (ByteBuffer) null, argValues);
    }
}
