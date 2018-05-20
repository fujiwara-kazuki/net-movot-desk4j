package net.movot.desk4j.Pointer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;

import javax.swing.JApplet;

public class App extends JApplet{

   Image img_src,img_gray,img_bin,img_temp;

   public void init(){

      //画像Faces.jpgをimg_srcとして読み込む
      img_src=readImageFile("Faces.jpg");
      //img_srcをグレイ化してimg_grayを作る
      img_gray=changeToGray(img_src);
      //img_grayを二値化してimg_binを作る
      img_bin=changeToBinary(img_gray);
      //テンプレート画像Temp.gifをimg_tempとして読み込む
      img_temp=readImageFile("Temp.gif");

   }

   //画像ファイルを読み込みImageクラスの画像にするメソッド
   public Image readImageFile(String filename){

      Image img=getImage(getDocumentBase(),filename);
      MediaTracker mtracker=new MediaTracker(this);
      mtracker.addImage(img,0);
      try{
         mtracker.waitForAll();
      }catch(Exception e){}
      return img;

   }

   //Imageクラスのカラー画像をImageクラスのグレイ画像にするメソッド
   public Image changeToGray(Image img_src){

      int i,r,g,b,d;
      Color color;

      int width=img_src.getWidth(this);
      int height=img_src.getHeight(this);
      int size=width*height;

      int[] rgb_src=new int[size];
      int[] rgb_gray=new int[size];

      PixelGrabber grabber=
         new PixelGrabber(img_src,0,0,width,height,rgb_src,0,width);
      try{
         grabber.grabPixels();  //画像imgを配列rgb_src[]に読み込む
      }catch(InterruptedException e){}

      //カラー画像をグレイ化する
      for(i=0;i<size;i++){
         color=new Color(rgb_src[i]);
         r=color.getRed();    //赤の成分を取り出す
         g=color.getGreen();  //緑の成分を取り出す
         b=color.getBlue();   //青の成分を取り出す
         d=(g*6+r*3+b)/10;    //グレイの成分を作る（NTSC方式準拠）
         color=new Color(d,d,d);
         rgb_gray[i]=color.getRGB();
      }

      Image img_gray=createImage(
         new MemoryImageSource(width,height,rgb_gray,0,width));
      return img_gray;

   }

   //Imageクラスのグレイ画像をImageクラスの二値画像にするメソッド
   public Image changeToBinary(Image img_gray){

      int i,r,g,b,d,s;
      Color color;

      int width=img_gray.getWidth(this);

      int height=img_gray.getHeight(this);
      int size=width*height;

      int[] rgb_gray=new int[size];
      int[] rgb_bin=new int[size];

      //画像imgを配列rgb_gray[]に読み込む
      PixelGrabber grabber=new PixelGrabber(img_gray,0,0,
         width,height,rgb_gray,0,width);
      try{
         grabber.grabPixels();
      }catch(InterruptedException e){}

      //ヒストグラム取得のためのhistogram[256]を生成し、ゼロに初期化
      int[] histogram=new int[256];
      for(i=0;i<256;i++)
         histogram[i]=0;

      //ヒストグラムを取得する
      for(i=0;i<size;i++){
         color=new Color(rgb_gray[i]);
         d=color.getBlue();  //グレイ画像なので、getRed()などでも良い
         histogram[d]++;
      }

      //頻度数の中間値を求め、thresholdとする
      i=0;
      s=0;
      while(s<size/2){
         s+=histogram[i++];
      }
      int threshold=i-1;  //-1で、最後に実行したi++の処理を元に戻す

      //グレイ画像のrgb_gray[]を二値化し、
      //ニ値画像のrgb_bin[]に変換する
      for(i=0;i<size;i++){
         color=new Color(rgb_gray[i]);
         d=color.getBlue();  //グレイ画像なので、getRed()などでも良い
         if(d>threshold) d=255;   //白
         else            d=0;     //黒
         color=new Color(d,d,d);
         rgb_bin[i]=color.getRGB();
     }

      Image img_bin=createImage(
         new MemoryImageSource(width,height,rgb_bin,0,width));
      return img_bin;

   }

   //テンプレートマッチングのメソッド
   public void searchMatching(Graphics g,int x0,int y0,
      Image img_bin,Image img_temp,int threshold){

      int x,y,i,j,s;
      Color color;

      // 被探索二値画像 img_bin の処理の準備
      int b_width=img_bin.getWidth(this);
      int b_height=img_bin.getHeight(this);
      int b_size=b_width*b_height;

      // 被探索二値画像 img_bin を一次元配列 rgb_bin[]に変換する
      int[] rgb_bin=new int[b_size];
      PixelGrabber grabber_bin=new PixelGrabber(
         img_bin,0,0,b_width,b_height,rgb_bin,0,b_width);
      try{
         grabber_bin.grabPixels();
      }catch(InterruptedException e){}

      // 一次元配列 rgb_bin[]を
      // 二次元二値配列 pixels_bin[][]に変換する
      byte[][] pixels_bin=new byte[b_height][b_width];
      for(y=0;y<b_height;y++)
         for(x=0;x<b_width;x++){
            color=new Color(rgb_bin[y*b_width+x]);
            if(color.getRed()==255) pixels_bin[y][x]=1;   //白
            else                    pixels_bin[y][x]=0;   //黒
         }

      // テンプレート画像 img_temp の処理の準備
      int t_width=img_temp.getWidth(this);
      int t_height=img_temp.getHeight(this);
      int t_size=t_width*t_height;

      // テンプレート画像 img_temp をrgb_temp[]に変換する
      int[] rgb_temp=new int[t_size];
      PixelGrabber grabber_temp=new PixelGrabber(
         img_temp,0,0,t_width,t_height,rgb_temp,0,t_width);
      try{
         grabber_temp.grabPixels();
      }catch(InterruptedException e){}

      // 一次元配列 rgb_temp[]を
      // 二次元三値配列 pixels_temp[][]に変換する
      byte[][] pixels_temp=new byte[t_height][t_width];
      for(y=0;y<t_height;y++)
         for(x=0;x<t_width;x++){
            color=new Color(rgb_temp[y*t_width+x]);

            //白
            if(color.getRed()==255)         pixels_temp[y][x]=1;
            //緑（Don't care）
            else if(color.getGreen()==255)  pixels_temp[y][x]=-1;
            //黒
            else                            pixels_temp[y][x]=0;
         }

      //マッチング個所を探索し、赤い枠で表示する　　　　　
      g.setColor(Color.red);
      for(y=0;y<b_height-t_height;y++)
         for(x=0;x<b_width-t_width;x++){
            s=0;
            for(j=0;j<t_height;j++)
               for(i=0;i<t_width;i++){
                  //比較しない
                  if(pixels_temp[j][i]==-1) continue;
                  //一致した
                  if(pixels_temp[j][i]==pixels_bin[y+j][x+i]){
                     //縦方向の中央部
                     if(i>t_width/3 && i<t_width*2/3) s+=4;
                     else s++;
                  }
               }
            if(s>threshold)
               g.drawRect(x0+x,y0+y,t_width,t_height);
         }
   }

   public void paint(Graphics g){

      //被探索画像img_srcを描画
      g.drawImage(img_src,10,0,img_src.getWidth(this),
         img_src.getHeight(this),this);
      //グレイ画像img_grayを描画
      g.drawImage(img_gray,340,0,img_gray.getWidth(this),
         img_gray.getHeight(this),this);
      //二値画像img_binを描画
      g.drawImage(img_bin,10,260,img_bin.getWidth(this),
         img_bin.getHeight(this),this);
      //テンプレート画像img_tempを描画
      g.drawImage(img_temp,340,260,img_temp.getWidth(this),
         img_temp.getHeight(this),this);

      //テンプレートマッチングを実施、結果を二値画像 img_bin 上に表示
      searchMatching(g,10,260,img_bin,img_temp,1300);

   }

}