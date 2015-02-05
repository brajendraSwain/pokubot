package aok.coc.util;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import aok.coc.util.coords.Area;
import aok.coc.util.coords.Clickable;
import aok.coc.util.w32.GDI32;
import aok.coc.util.w32.User32;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;

public class RobotUtils {

	private static final Logger	logger				= Logger.getLogger(RobotUtils.class.getName());

	public static final String	WORKING_DIR			= System.getProperty("user.dir");
	public static final int		SCREEN_WIDTH		= Toolkit.getDefaultToolkit().getScreenSize().width;
	public static final int		SCREEN_HEIGHT		= Toolkit.getDefaultToolkit().getScreenSize().height;
	public static final String	SYSTEM_OS			= System.getProperty("os.name");
	public static final String	USER_NAME			= System.getProperty("user.name");
	public static final String	USER_HOME_DIR		= System.getProperty("user.home");

	private static Robot		r;

	public static Random		random				= new Random();

	static {
		try {
			r = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	// user32
	public static final int		WM_COMMAND			= 0x111;
	public static final int		WM_LBUTTONDOWN		= 0x201;
	public static final int		WM_LBUTTONUP		= 0x202;
	public static final int		WM_LBUTTONDBLCLK	= 0x203;
	public static final int		WM_RBUTTONDOWN		= 0x204;
	public static final int		WM_RBUTTONUP		= 0x205;
	public static final int		WM_RBUTTONDBLCLK	= 0x206;
	public static final int		WM_KEYDOWN			= 0x100;
	public static final int		WM_KEYUP			= 0x101;
	public static final int		WM_MOUSEWHEEL		= 0x20A;

	private static HWND			handle				= null;

	public static void setupWin32(HWND handler) {
		RobotUtils.handle = handler;
	}

	public static boolean clientToScreen(POINT clientPoint) {
		return User32.INSTANCE.ClientToScreen(handle, clientPoint);
	}

	public static void zoomUp(int notch) throws InterruptedException {
		logger.info("Zooming out...");
		for (int i = 0; i < notch; i++) {
			User32.INSTANCE.SendMessage(handle, WM_KEYDOWN, 0x28, 0X1500001);
			User32.INSTANCE.SendMessage(handle, WM_KEYDOWN, 0X11, 0X11d0001);
			Thread.sleep(1000);
		}
	}

	public static void zoomUp() throws InterruptedException {
		zoomUp(14);
	}

	public static void leftClick(Clickable clickable, int sleepInMs) throws InterruptedException {
		boolean randomize = clickable != Clickable.UNIT_FIRST_RAX;
		leftClickWin32(clickable.getX(), clickable.getY(), randomize);
		Thread.sleep(sleepInMs + random.nextInt(sleepInMs));
	}

	public static void leftClick(int x, int y) {
		leftClickWin32(x, y, false);
	}

	public static void leftClick(int x, int y, int sleepInMs) throws InterruptedException {
		leftClickWin32(x, y, false);
		Thread.sleep(sleepInMs + random.nextInt(sleepInMs));
	}

	private static void leftClickWin32(int x, int y, boolean randomize) {
		// randomize coordinates little bit
		if (randomize) {
			x += -1 + random.nextInt(3);
			y += -1 + random.nextInt(3);
		}
		logger.finest("clicking " + x + " " + y);
		int lParam = makeParam(x, y);
		User32.INSTANCE.SendMessage(handle, WM_LBUTTONDOWN, 0x00000001, lParam);
		User32.INSTANCE.SendMessage(handle, WM_LBUTTONUP, 0x00000000, lParam);
	}

	private static int makeParam(int low, int high) {
		// to work for negative numbers
		return (high << 16) | ((low << 16) >>> 16);
	}

	public static void sleepTillClickableIsActive(Clickable clickable) throws InterruptedException {
		while (true) {
			if (isClickableActive(clickable)) {
				return;
			}
			Thread.sleep(random.nextInt(250) + 750);
		}
	}

	public static void sleepRandom(int i) throws InterruptedException {
		Thread.sleep(i + random.nextInt(i));
	}

	private static void msgBox(String Text, String Title) {
		JOptionPane.showMessageDialog(null, Text, Title, JOptionPane.PLAIN_MESSAGE); //Show message box
	}

	public static void msgBox(String Text) {
		msgBox(Text, "");
	}

	public static boolean confirmationBox(String msg, String title) {
		int result = JOptionPane.showConfirmDialog(null, msg, title, JOptionPane.YES_NO_OPTION);

		if (result == JOptionPane.YES_OPTION) {
			return true;
		} else {
			return false;
		}
	}

	public static BufferedImage screenShotBackGround(int x1, int y1, int x2, int y2) {
		HDC hdcWindow = User32.INSTANCE.GetDC(handle);
		HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);

		int width = x2 - x1;
		int height = y2 - y1;

		HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, width, height);

		HANDLE hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
		GDI32.INSTANCE.BitBlt(hdcMemDC, 0, 0, width, height, hdcWindow, x1, y1, new DWORD(0x00CC0020));

		GDI32.INSTANCE.SelectObject(hdcMemDC, hOld);
		GDI32.INSTANCE.DeleteDC(hdcMemDC);

		BITMAPINFO bmi = new BITMAPINFO();
		bmi.bmiHeader.biWidth = width;
		bmi.bmiHeader.biHeight = -height;
		bmi.bmiHeader.biPlanes = 1;
		bmi.bmiHeader.biBitCount = 32;
		bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

		Memory buffer = new Memory(width * height * 4);
		GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS);

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, width, height, buffer.getIntArray(0, width * height), 0, width);

		GDI32.INSTANCE.DeleteObject(hBitmap);
		User32.INSTANCE.ReleaseDC(handle, hdcWindow);

		return image;
	}

	public static BufferedImage screenShot(Area area) {
		return screenShotBackGround(area.getX1(), area.getY1(), area.getX2(), area.getY2());
	}

	public static File saveScreenShot(Area area, String filePathFirst, String... filePathRest) throws IOException {
		return saveScreenShot(area.getX1(), area.getY1(), area.getX2(), area.getY2(), filePathFirst, filePathRest);
	}

	public static File saveScreenShot(int x1, int y1, int x2, int y2, String filePathFirst, String... filePathRest) throws IOException {
		Path path = Paths.get(filePathFirst, filePathRest).toAbsolutePath();
		String fileName = path.getFileName().toString();
		if (!(path.getFileName().toString().toLowerCase().endsWith(".png"))) {
			fileName = path.getFileName().toString() + ".png";
		}
		BufferedImage img = screenShotBackGround(x1, y1, x2, y2);
		File file = new File(path.getParent().toString(), fileName);
		if (!file.getParentFile().isDirectory()) {
			file.getParentFile().mkdirs();
		}
		ImageIO.write(img, "png", file);
		return file;
	}

	public static Color pixelGetColor(int x, int y) {
		BufferedImage image = screenShotBackGround(x, y, x + 1, y + 1);
		return new Color(image.getRGB(0, 0));
//		POINT point = new POINT(x, y);
//		clientToScreen(point);
//		Color pixel = r.getPixelColor(point.x, point.y);
//		return pixel;
	}

	public static boolean isClickableActive(Clickable clickable) {
		if (clickable.getColor() == null) {
			throw new IllegalArgumentException(clickable.name());
		}

		int tarColor = clickable.getColor().getRGB();
		int actualColor = pixelGetColor(clickable.getX(), clickable.getY()).getRGB();
		if (clickable == Clickable.BUTTON_RAX_MAX_TRAIN || clickable == Clickable.BUTTON_RAX_TRAIN) {
			logger.finest("isClickableActive: " + clickable.name() + " " + Integer.toHexString(tarColor)
							+ " " + Integer.toHexString(actualColor));
		}
		return compareColor(tarColor, actualColor, 5);
	}

	public static boolean compareColor(int c1, int c2, int var) {
		int r1 = (c1 >> 16) & 0xFF;
		int r2 = (c2 >> 16) & 0xFF;

		int g1 = (c1 >> 8) & 0xFF;
		int g2 = (c2 >> 8) & 0xFF;

		int b1 = (c1 >> 0) & 0xFF;
		int b2 = (c2 >> 0) & 0xFF;

		if (Math.abs(r1 - r2) > var || Math.abs(g1 - g2) > var || Math.abs(b1 - b2) > var) {
			return false;
		} else {
			return true;
		}
	}

}