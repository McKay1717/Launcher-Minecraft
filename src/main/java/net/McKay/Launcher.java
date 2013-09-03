package net.McKay;

import java.applet.Applet;
import java.applet.AppletStub;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.VolatileImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

public class Launcher extends Applet
  implements Runnable, AppletStub, MouseListener
{
  private static final long serialVersionUID = 1L;
  public Map<String, String> customParameters = new HashMap<String, String>();
  private GameUpdater gameUpdater;
  private boolean gameUpdaterStarted = false;
  private Applet applet;
  private Image bgImage;
  private boolean active = false;
  private int context = 0;
  private boolean hasMouseListener = false;
  private VolatileImage img;
  public String user = this.getParameter("userName");
  public String sesion = getParameter("sessionId");

  public boolean isActive()
  {
    if (this.context == 0) {
      this.context = -1;
      try {
        if (getAppletContext() != null) this.context = 1; 
      }
      catch (Exception localException) {
      }
    }
    if (this.context == -1) return this.active;
    return super.isActive();
  }

  public void init(String userName, String latestVersion, String downloadTicket, String sessionId)
  {
    try {
      this.bgImage = ImageIO.read(LoginForm.class.getResource("dirt.png")).getScaledInstance(32, 32, 16);
    } catch (IOException e) {
      e.printStackTrace();
    }

    this.customParameters.put("username", userName);
    this.customParameters.put("sessionid", sessionId);

    this.gameUpdater = new GameUpdater(latestVersion, "minecraft.jar?user=" + userName + "&ticket=" + downloadTicket);
  }

  public boolean canPlayOffline() {
    return this.gameUpdater.canPlayOffline();
  }

  public void init() {
    if (this.applet != null) {
      this.applet.init();
      return;
    }
    init(getParameter("userName"), getParameter("latestVersion"), getParameter("downloadTicket"), getParameter("sessionId"));
  }
  public void launchMinecraft()
	{
		try {
			// laucnher folder
			String mcpath = Util.getWorkingDirectory().getAbsolutePath();
			// nickname
			String nick = this.getParameter("username");
			// RAM
			String mem = "1024M";
			// minecraft launch version

			// location of jar file
			File workDir = Util.getWorkingDirectory();
			File assetDir = new File(workDir, "assets");
			File binDir = new File(workDir, "bin");
			File nativeDir = new File(binDir, "natives");
			File jar = new File(binDir, "darkube.jar");
			// libs and java locations
			String cps;
			String java = '"'+System.getProperty("java.home");
			if (System.getProperty("os.name").toLowerCase().contains("win")) {
				cps = ";";
				java+=File.separator+"bin"+File.separator+"javaw.exe"+'"';
			} else {
				cps = ":";
				java+=File.separator+"bin"+File.separator+"java";
			}


			// mods tweaks type



			ProcessBuilder pb = new ProcessBuilder();
			pb.directory(new File(mcpath).getCanonicalFile());
			List<String> cc = new ArrayList<String>();
			cc.add(java);
			cc.add("-Xmx" + mem);
	 //	cc.add("-XX:+UseFastAccessorMethods -XX:+AggressiveOpts  -XX:+DisableExplicitGC -XX:+UseAdaptiveGCBoundary -XX:MaxGCPauseMillis=500 -XX:SurvivorRatio=16 -XX:+UseParallelGC -XX:UseSSE=3 -XX:ParallelGCThreads=4");
			cc.add("-Djava.library.path=" + nativeDir.getAbsolutePath());
			cc.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
			cc.add("-Dfml.ignorePatchDiscrepancies=true");
			cc.add("-cp");
			cc.add(jar.getAbsolutePath());
			//minecraft with tweaks
			cc.add("net.minecraft.launchwrapper.Launch");
			cc.add("--tweakClass");
			// minecraft with forge
			cc.add("cpw.mods.fml.common.launcher.FMLTweaker");


			cc.add("--username");
			cc.add(nick);
			cc.add("--session");
			cc.add(getParameter("sessionid"));
			cc.add("--version");
			cc.add("1.6.2");
			cc.add("--gameDir");
			cc.add(mcpath);
			cc.add("--assetsDir");
			cc.add(mcpath + File.separator + "assets");


			pb.command(cc);
			pb.inheritIO(); // Do not remove this
			Process pro = pb.start();
			System.out.println(cc.toString());
	
			InputStream is = pro.getInputStream();
		    InputStreamReader isr = new InputStreamReader(is);
		    BufferedReader br = new BufferedReader(isr);
		    String line;
		    while ((line = br.readLine()) != null) {
		      System.out.println(line);
		    }
		    //SpoutcraftLauncher.flush();
		    System.exit(0);

		} catch (Exception e) {

		}
	}

  public void start() {
    if (this.applet != null) {
      this.applet.start();
      return;
    }
    if (this.gameUpdaterStarted) return;

    Thread t = new Thread() {
      public void run() {
        Launcher.this.gameUpdater.run();
        if (!Launcher.this.gameUpdater.fatalError)
        	  launchMinecraft();
      }
    };
    t.setDaemon(true);
    t.start();

    t = new Thread() {
      public void run() {
        while (Launcher.this.applet == null) {
          Launcher.this.repaint();
          try {
            Thread.sleep(10L);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    };
    t.setDaemon(true);
    t.start();

    this.gameUpdaterStarted = true;
  }

  public void stop() {
    if (this.applet != null) {
      this.active = false;
      this.applet.stop();
      return;
    }
  }

  public void destroy() {
    if (this.applet != null) {
      this.applet.destroy();
      return;
    }
  }

  public void replace(Applet applet) {
    this.applet = applet;
    applet.setStub(this);
    applet.setSize(getWidth(), getHeight());

    setLayout(new BorderLayout());
    add(applet, "Center");

    applet.init();
    this.active = true;
    applet.start();
    validate();
  }

  public void update(Graphics g)
  {
    paint(g);
  }

  public void paint(Graphics g2) {
    if (this.applet != null) return;

    int w = getWidth() / 2;
    int h = getHeight() / 2;
    if ((this.img == null) || (this.img.getWidth() != w) || (this.img.getHeight() != h)) {
      this.img = createVolatileImage(w, h);
    }

    Graphics g = this.img.getGraphics();
    for (int x = 0; x <= w / 32; x++) {
      for (int y = 0; y <= h / 32; y++)
        g.drawImage(this.bgImage, x * 32, y * 32, null);
    }
    if (this.gameUpdater.pauseAskUpdate) {
      if (!this.hasMouseListener) {
        this.hasMouseListener = true;
        addMouseListener(this);
      }
      g.setColor(Color.LIGHT_GRAY);
      String msg = "New update available";
      g.setFont(new Font(null, 1, 20));
      FontMetrics fm = g.getFontMetrics();
      g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 - fm.getHeight() * 2);

      g.setFont(new Font(null, 0, 12));
      fm = g.getFontMetrics();

      g.fill3DRect(w / 2 - 56 - 8, h / 2, 56, 20, true);
      g.fill3DRect(w / 2 + 8, h / 2, 56, 20, true);

      msg = "Would you like to update?";
      g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 - 8);

      g.setColor(Color.BLACK);
      msg = "Yes";
      g.drawString(msg, w / 2 - 56 - 8 - fm.stringWidth(msg) / 2 + 28, h / 2 + 14);
      msg = "Not now";
      g.drawString(msg, w / 2 + 8 - fm.stringWidth(msg) / 2 + 28, h / 2 + 14);
    }
    else
    {
      g.setColor(Color.LIGHT_GRAY);

      String msg = "Mise à jour de - -McKay-";
      if (this.gameUpdater.fatalError) {
        msg = "Failed to launch";
      }

      g.setFont(new Font(null, 1, 20));
      FontMetrics fm = g.getFontMetrics();
      g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 - fm.getHeight() * 2);

      g.setFont(new Font(null, 0, 12));
      fm = g.getFontMetrics();
      msg = this.gameUpdater.getDescriptionForState();
      if (this.gameUpdater.fatalError) {
        msg = this.gameUpdater.fatalErrorDescription;
      }

      g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 + fm.getHeight() * 1);
      msg = this.gameUpdater.subtaskMessage;
      g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 + fm.getHeight() * 2);

      if (!this.gameUpdater.fatalError) {
        g.setColor(Color.black);
        g.fillRect(64, h - 64, w - 128 + 1, 5);
        g.setColor(new Color(32768));
        g.fillRect(64, h - 64, this.gameUpdater.percentage * (w - 128) / 100, 4);
        g.setColor(new Color(2138144));
        g.fillRect(65, h - 64 + 1, this.gameUpdater.percentage * (w - 128) / 100 - 2, 1);
      }
    }

    g.dispose();

    g2.drawImage(this.img, 0, 0, w * 2, h * 2, null);
  }

  public void run() {
  }

  public String getParameter(String name) {
    String custom = (String)this.customParameters.get(name);
    if (custom != null) return custom; try
    {
      return super.getParameter(name);
    } catch (Exception e) {
      this.customParameters.put(name, null);
    }return null;
  }

  public void appletResize(int width, int height)
  {
  }

  public URL getDocumentBase() {
    try {
      return new URL("http://www.minecraft.net/game/");
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void mouseClicked(MouseEvent arg0) {
  }

  public void mouseEntered(MouseEvent arg0) {
  }

  public void mouseExited(MouseEvent arg0) {
  }

  public void mousePressed(MouseEvent me) {
    int x = me.getX() / 2;
    int y = me.getY() / 2;
    int w = getWidth() / 2;
    int h = getHeight() / 2;

    if (contains(x, y, w / 2 - 56 - 8, h / 2, 56, 20)) {
      removeMouseListener(this);
      this.gameUpdater.shouldUpdate = true;
      this.gameUpdater.pauseAskUpdate = false;
      this.hasMouseListener = false;
    }
    if (contains(x, y, w / 2 + 8, h / 2, 56, 20)) {
      removeMouseListener(this);
      this.gameUpdater.shouldUpdate = false;
      this.gameUpdater.pauseAskUpdate = false;
      this.hasMouseListener = false;
    }
  }

  private boolean contains(int x, int y, int xx, int yy, int w, int h) {
    return (x >= xx) && (y >= yy) && (x < xx + w) && (y < yy + h);
  }

  public void mouseReleased(MouseEvent arg0)
  {
  }
}