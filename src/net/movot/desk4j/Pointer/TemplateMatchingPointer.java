package net.movot.desk4j.Pointer;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class TemplateMatchingPointer extends Pointer {

	static int xpos = 0; //テンプレートが一致したときのx座標
	static int ypos = 0; //テンプレートが一致したときのy座標
	static boolean matchFlg = false;

	public static void main(String[] args) {

		long start = System.currentTimeMillis();

		//テンプレート、被探索画像の読み込み
		BufferedImage tempImg;//  = imgRead("pic/tmp.jpg");
		BufferedImage srchImg;//  = imgRead("pic/image.jpg");

		// ここから
		try {
			// キャプチャの範囲
			Rectangle bounds = new Rectangle(0, 0, 1000, 1000);
			// これで画面キャプチャ
			Robot robot = new Robot();
			srchImg = robot.createScreenCapture(bounds);

			// キャプチャの範囲
			bounds = new Rectangle(100, 100, 200, 200);
			// これで画面キャプチャ
			tempImg = robot.createScreenCapture(bounds);

			//テンプレート、被探索画像を配列に変換
			int[][] temp = imgToArray(tempImg);
			int[][] srch = imgToArray(srchImg);

			//テンプレート、被探索画像をグレースケールに変換
			int[][] g_temp = trans_grayscale(temp);
			int[][] g_srImg = trans_grayscale(srch);

			//テンプレートマッチングを行う
			templateMatcing(g_temp, g_srImg);

			long end = System.currentTimeMillis();
			System.out.println((end - start) / 1000 + "s");

			//テンプレートマッチングした結果を表示する
			DispFrame frame;
			if (matchFlg) {
				frame = new DispFrame(srchImg, tempImg, xpos, ypos);
			} else {
				frame = new DispFrame(srchImg, tempImg);
				System.out.println("No Matching!!");
			}
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
		} catch (AWTException e) {
			e.printStackTrace();
		}
		// ここまで
	}

	//画像ファイルを読み込むメソッド
	public static BufferedImage imgRead(String file_path) {
		BufferedImage img = null;

		try {
			img = ImageIO.read(new File(file_path));
			return img;
		} catch (Exception e) {
			return null;
		}
	}

	//画像ファイルを２次元配列に変換するメソッド
	public static int[][] imgToArray(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();
		int[][] imgA = new int[height][width];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				imgA[y][x] = img.getRGB(x, y); //画像上の(x, y)におけるRGB値を取得
			}
		}
		return imgA;
	}

	//RGB値からグレースケールに変換するメソッド
	public static int[][] trans_grayscale(int[][] img) {
		int width = img[0].length;
		int height = img.length;
		int[][] gray_img = new int[height][width];

		for (int y = 0; y < img.length; y++) {
			for (int x = 0; x < img[0].length; x++) {
				int rgb = img[y][x] - 0xFF000000; //アルファ値を取り除く
				int b = (rgb & 0xFF); //青の成分を取得
				int g = (rgb & 0xFF00) >> 8; //緑の成分を取得
				int r = (rgb & 0xFF0000) >> 16; //赤の成分を取得
				int gray = (b + g + r) / 3; //グレーの値に変換
				gray_img[y][x] = gray;
			}
		}
		return gray_img;
	}

	//テンプレートマッチングするメソッド
	public static void templateMatcing(int[][] temp, int[][] srch) {
		int tempW = temp[0].length;
		int tempH = temp.length;
		int srchW = srch[0].length;
		int srchH = srch.length;
		int min_ssd = Integer.MAX_VALUE;//最小の二乗誤差の和
		min_ssd = 10000000;
		int dif = 0;//非探索画像とテンプレートのピクセル単位での差

		for (int y = 0; y < srchH - tempH; y++) {
			for (int x = 0; x < srchW - tempW; x++) {
				int ssd = 0; //二乗誤差の和(SSD)
				flag: for (int yt = 0; yt < tempH; yt++) {
					for (int xt = 0; xt < tempW; xt++) {
						dif = srch[y + yt][x + xt] - temp[yt][xt];
						ssd += dif * dif; //もし、min_ssdの値を超えたら、次の位置に移る。
						if (ssd > min_ssd) {
							continue flag;
						}
					}
				}

				if (min_ssd > ssd) {
					xpos = x;
					ypos = y;
					min_ssd = ssd;
					matchFlg = true;
				}

			}
		}
		System.out.println("Min_SSD =" + min_ssd); //SSDの最小値を表示
		System.out.println("position=" + xpos + "," + ypos); //テンプレートの一致した座標を表示
	}
}

class DispFrame extends JFrame {
	BufferedImage srch; //フレーム上に表示するための被探索画像
	int xpos = 0; //テンプレートが一致したx座標
	int ypos = 0; //テンプレートが一致したy座標
	int temp_width;
	int temp_height;
	boolean matchFlg = true;

	DispFrame(BufferedImage srch, BufferedImage temp, int xpos, int ypos) {
		this.xpos = xpos;
		this.ypos = ypos;
		this.srch = srch;

		temp_width = temp.getWidth();
		temp_height = temp.getHeight();

		setSize(srch.getWidth(), srch.getHeight());
		setTitle("RESULT");
	}

	DispFrame(BufferedImage srch, BufferedImage temp) {
		this.srch = srch;
		matchFlg = false;

		temp_width = temp.getWidth();
		temp_height = temp.getHeight();

		setSize(srch.getWidth(), srch.getHeight());
		setTitle("RESULT");
	}

	//非探索画像上のテンプレートが一致したところに四角を囲むメソッド
	public void paint(Graphics g) {
		if (matchFlg) {
			Graphics2D off = srch.createGraphics();
			off.setColor(new Color(0, 0, 255)); //四角の色を青にする
			off.drawRect(xpos, ypos, temp_width, temp_height);
		}
		g.drawImage(srch, 0, 0, this);
	}

}