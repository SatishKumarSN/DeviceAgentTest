package com.motorola.deviceagenttest;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

@SuppressWarnings("serial")
public class DeviceAgent extends JFrame implements ActionListener {

	private final String SDK_PATH = "D://adt-bundle-windows-x86_64-20130917//sdk//platform-tools//adb.exe";
	private final String DEVICE_DESTINATION_PATH = "/data/local/tmp";

	private String serverIPAddress;
	private String userName;
	private String password;
	private int dbType;
	private DBAccess dbAcess;

	private JTextArea resultsArea;
	private String openCon = "";
	boolean HostDeviceDataNotAvailable = false;
	boolean WaitForShutDownStop = false;
	boolean HasAgentExited = false;
	boolean TestStartedFlag = false;
	boolean FTPDone = false;

	String DOWNLOAD_JAR_PATH = "D:\\downloaded_jar\\";
	String deviceId;
	String hostIP = "157.235.208.246";
	String deviceName;
	Thread ss;
	Thread ff;
	DBAccess dB_Timer_MonitorStart = null;
	DBAccess dB_Timer_MonitorExit = null;
	int ETR;
	int myETR = 0;

	Timer timer2 = new Timer();
	MonitorTestExecutionStop_TimerTask MonitorTestExecutionStop_TimerTask = new MonitorTestExecutionStop_TimerTask();

	public DeviceAgent() {
		JPanel panel = new JPanel();
		panel.setBackground(Color.LIGHT_GRAY);
		JFrame frame = new JFrame("DeviceAgent");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.getContentPane().add(panel);
		panel.setLayout(null);

		resultsArea = new JTextArea();
		resultsArea.setBounds(61, 26, 282, 45);
		panel.add(resultsArea);

		JButton startBtn = new JButton("Start");
		startBtn.setBounds(90, 109, 89, 23);
		startBtn.addActionListener(this);
		panel.add(startBtn);

		JButton exitBtn = new JButton("Exit");
		exitBtn.setBounds(218, 109, 89, 23);
		exitBtn.addActionListener(this);
		panel.add(exitBtn);
		frame.getContentPane().add(panel);
		frame.setSize(400, 200);
		frame.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println(e.getActionCommand());

		if ("Start".equals(e.getActionCommand())) {

			try {
				Class.forName("net.sourceforge.jtds.jdbc.Driver");
			} catch (ClassNotFoundException e1) {
				System.out.println("Unable to load the Jtds.jdbc driver");
				e1.printStackTrace();
				System.exit(0);
			}
			dbAcess = new DBAccess();

			if (!checkForIPAddr()) {
				System.out.println("Ip address file is not exist ...");
				startDeviceAgentTest();
			} else {
				new CheckConnectionTask().execute();
			}
		} else if ("Exit".equals(e.getActionCommand())) {
			System.exit(0);
		}
	}

	class CheckConnectionTask extends SwingWorker<String, String> {

		@Override
		protected String doInBackground() throws Exception {
			System.out.println("Do in background");
			publishProgress("Running Device Agent..");

			if (dbAcess.CheckConnection()) {
				openCon = "Database_Opened";
			}

			if (!openCon.equals("Database_Opened")) {
				publishProgress("Unable to connect to Database Server.\r\nPlease make sure Device has valid IP and start DeviceAgent again.");
				System.out
						.println("Unable to connect to Database Server.\r\nPlease make sure Device has valid IP and start DeviceAgent again.");
				System.exit(0);
			} else {

				int retVal = 0;
				try {
					retVal = checkDeviceInfo();
					System.out.println("Return Value " + retVal);
				} catch (SQLException e) {
					e.printStackTrace();
				}
				if (retVal == 0) {
					System.out.println("Calling for Thread Timer");
					WaitforHostToStart waitStartThread = new WaitforHostToStart();
					waitStartThread.run();
					downloadApp();
				} else if (retVal == 1) {
					try {
						ManageIncompleteTestRun();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("Invalid Data ");
				}
			}
			return openCon;
		}
	}

	private int checkDeviceInfo() throws SQLException {
		int Ret = 100;
		deviceId = getDeviceIds();
		deviceName = deviceId;
		dbAcess.DeviceID = deviceId;
		try {
			if (dbAcess.checkDeviceIdTableExistenceInDB(deviceId)) {
				if (dbAcess
						.CheckDeviceIdEntryExistenceforHostDeviceTable(deviceId)) {
					ClearDeviceInfo();

					System.out.println("Device Id  " + deviceId);
					dbAcess.InsertDeviceInfo(hostIP.toString(), deviceId,
							deviceName, 1);
					int ETR = dbAcess.CheckTestRunStatus();
					if (ETR == 6) {
						Ret = 0;
					} else {
						Ret = 1;
					}
				} else {
					ClearDeviceInfo();
					dbAcess.InsertDeviceInfo(hostIP.toString(), deviceId,
							deviceName, 0);
					Ret = 0;
				}
			} else {
				ClearDeviceInfo();
				dbAcess.InsertDeviceInfo(hostIP.toString(), deviceId,
						deviceName, 0);
				Ret = 0;
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			ExitDeviceAgent();
		}
		return Ret;
	}

	public class WaitforHostToStart implements Runnable {
		public void run() {
			try {
				publishProgress("Waiting for Test Run to Start from Host");
				while (true) {
					ETR = dbAcess.CheckTestRunStatus();
					System.out.println("ETR Retrun " + ETR);
					if (ETR == 1) {
						TestStartedFlag = true;
						System.out.println("Setting the TestStarted Flag ...."
								+ TestStartedFlag);
						break;
					} else if (ETR == 5) {
						dbAcess.updateDeviceStatus(deviceId,
								"Test Execution has been stopped From Host.");
						ClearDeviceInfo();
						ExitDeviceAgent();
						break;
					}
					Thread.sleep(2000);
				}
			} catch (Exception e) {

			}
		}
	}

	private void startDeviceAgentTest() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				DeviceAgentTest ex = new DeviceAgentTest();
				ex.setVisible(true);
			}
		});
	}

	public void publishProgress(String msg) {
		resultsArea.setText(msg);
	}

	private String getDeviceIds() {
		try {
			Process process = Runtime.getRuntime().exec("adb devices");
			BufferedReader in = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line = null;

			Pattern pattern = Pattern
					.compile("^([a-zA-Z0-9\\-]+)(\\s+)(device)");
			Matcher matcher;

			while ((line = in.readLine()) != null) {
				if (line.matches(pattern.pattern())) {
					matcher = pattern.matcher(line);
					if (matcher.find()) {
						System.out.println(matcher.group(1));
						deviceId = matcher.group(1);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return deviceId;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new DeviceAgent();

			}
		});
	}

	public boolean checkForIPAddr() {
		try {
			File file = new File("D:\\IP_PATH\\IPAddr.txt");
			if (file.exists()) {

				FileInputStream out = new FileInputStream(file);
				BufferedReader r = new BufferedReader(new InputStreamReader(
						out, Charset.forName("UTF-8")));
				serverIPAddress = r.readLine();
				userName = r.readLine();
				password = r.readLine();
				dbType = Integer.valueOf(r.readLine());
				dbAcess.DBIP = serverIPAddress;
				dbAcess.DBUser = userName;
				dbAcess.DBPass = password;
				dbAcess.dbType = dbType;
				System.out.println(serverIPAddress);
				System.out.println(userName);
				System.out.println(password);
				System.out.println(dbType);
				r.close();
				out.close();
			} else {
				return false;
			}
		} catch (Exception e1) {
			return false;
		}
		return true;
	}

	public class MonitorTestExecutionStop_TimerTask extends TimerTask {

		public void run() {

			if (!HasAgentExited) {
				myETR = 0;
				try {
					myETR = dbAcess.CheckTestRunStatus();

					if (myETR == 5) {
						timer2.cancel();
						dbAcess.updateDeviceStatus(deviceId,
								"Test Execution has been stopped From Host.");
						ClearDeviceInfo();
						ExitDeviceAgent();
					} else if ((myETR == 3) || (myETR == 4)) {
						timer2.cancel();
						if (myETR == 4) {
							System.out
									.println("Test run has completed for this device but Host is still maintaining the connection with this device");
						}
						ClearDeviceInfo();
						ExitDeviceAgent();
					}
				} catch (Exception e1) {
					System.out.println(e1.getMessage());
					ExitDeviceAgent();
				}
			}
		}
	}

	private void downloadApp() {
		System.out.println("Test started from the Host.... " + TestStartedFlag);
		if (!HasAgentExited && TestStartedFlag) {
			publishProgress("Test Started");
			timer2.schedule(MonitorTestExecutionStop_TimerTask, (2 * 1000));
			try {
				DownloadAndLaunch();
				String temp = "Download Complete! Starting Test Application.";
				dbAcess.updateDeviceStatus(deviceId, temp);
				ExitDeviceAgent();
			} catch (InterruptedException e) {
				e.printStackTrace();
				publishProgress("Exception occured in downloading Jar File"
						+ e.getMessage());
			}
		} else {
			System.out.println("Something worng in Device Agent....");
		}
	}

	private void DownloadAndLaunch() throws InterruptedException {
		System.out.println("Download and launch");
		try {
			GetHostDeviceData();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		DownloadFiles();
		if (FTPDone == true) {
			dbAcess.updateTime("UPDATE HostDevice SET FTPStatus=1 WHERE DeviceMAC='"
					+ deviceId + "'");
			StartTestApplication();
			Thread.sleep(5000);
			System.out.println("Exiting Device agent successfully");
			ExitDeviceAgent();
		} else {
			if (HostDeviceDataNotAvailable == true) {
				System.out
						.println("Download Files Failed as TestRun Data not available!\r\nPlease warmboot the device and make sure device has a valid IP prior to starting DeviceAgent again!DownloadAndLaunch");
				ExitDeviceAgent();
			} else {
				System.out
						.println("Download Files Failed!\r\nPlease warmboot the device and make sure device has a valid IP prior to starting DeviceAgent again!DownloadAndLaunch");
				ExitDeviceAgent();
			}
		}
	}

	private void DownloadFiles() {
		System.out.println("Download Files");
		int port = 21;
		boolean success = false;

		FTPClient ftpClient = new FTPClient();

		try {
			String temp1 = "Connecting to FTP Server: " + dbAcess.ftpIP;
			dbAcess.updateDeviceStatus(deviceId, temp1);
			publishProgress("Connecting to FTP Server: " + dbAcess.ftpIP);
			System.out.println("Connecting to FTP Server: " + dbAcess.ftpIP);

			ftpClient.connect(dbAcess.ftpIP, port);
			ftpClient.login(dbAcess.ftpUserName, dbAcess.ftpPswd);
			ftpClient.enterLocalPassiveMode();
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			ftpClient.setBufferSize(1024 * 1024);
			String temp2 = "Connected to FTP Server: " + dbAcess.ftpIP;
			dbAcess.updateDeviceStatus(deviceId, temp2);
			publishProgress("Connected to FTP Server" + dbAcess.ftpIP);
			System.out.println("Connected to FTP Server" + dbAcess.ftpIP);

			String remoteFile = dbAcess.ExeName;
			System.out.println("Destination path " + DOWNLOAD_JAR_PATH
					+ remoteFile);
			File downloadFile = new File(DOWNLOAD_JAR_PATH + remoteFile);

			OutputStream outputStream1 = new BufferedOutputStream(
					new FileOutputStream(downloadFile));
			success = ftpClient.retrieveFile(remoteFile, outputStream1);
			publishProgress("Downloading....");

			System.out.println("Downloading");

			outputStream1.close();

			if (success) {
				publishProgress("Download success");
				System.out.println("Download success");
			}

		} catch (SocketException ex) {
			System.out.println("Error: " + ex.getMessage());
			ex.printStackTrace();
		} catch (UnknownHostException ex) {
			System.out.println("Error: " + ex.getMessage());
			ex.printStackTrace();
		} catch (IOException ex) {
			System.out.println("Error: " + ex.getMessage());
			ex.printStackTrace();
		}

		finally {
			try {
				if (ftpClient.isConnected()) {
					ftpClient.logout();
					ftpClient.disconnect();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		FTPDone = true;
	}

	public void StartTestApplication() {
		try {
			if (myETR != 5) {
				String temp = "Download Complete! Starting Test Application.";
				dbAcess.updateDeviceStatus(deviceId, temp);
				publishProgress("Download Complete! Starting OObTest Application.");

				if (!pushJartoDevice(dbAcess.ExeName)) {
					System.out.println("Unable to push Jar to device ..");
					dbAcess.updateDeviceStatus(deviceId,
							"Device Agent stopped Test Execution unable to push jar to device.");
					publishProgress("Unable to Start the Application..");
					timer2.cancel();
					ClearDeviceInfo();
					ExitDeviceAgent();
					System.exit(0);
				}
				String filePath = null;
				Runtime.getRuntime().exec("java -jar" + filePath);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private boolean pushJartoDevice(String jarName) {
		jarName = jarName.trim();
		String[] pushCmdStr = { SDK_PATH, "push", DOWNLOAD_JAR_PATH + jarName,
				DEVICE_DESTINATION_PATH };
		for (String tmp : pushCmdStr) {
			System.out.println(tmp);
		}
		return excuteTestCase(pushCmdStr);
	}

	private boolean excuteTestCase(String[] cmdRunStr) {
		if (cmdRunStr == null) {
			System.out.println("Run time command is null");
			return false;
		}
		Runtime runtime = Runtime.getRuntime();
		try {
			Process process = runtime.exec(cmdRunStr);
			return readConsoleOutput(process);
		} catch (IOException e) {
			System.out.println("Io Exception while running command");
			e.printStackTrace();
			return false;
		}
	}

	private boolean readConsoleOutput(Process process) {
		BufferedReader input = new BufferedReader(new InputStreamReader(
				process.getInputStream()));
		String line;
		try {
			while ((line = input.readLine()) != null) {
				line = line.trim();
				if (!line.isEmpty()) {
					System.out.println(line);
				}
			}
			input.close();
		} catch (IOException e) {
			System.out.println("Io Exception while Reading output");
			e.printStackTrace();
			return false;
		}
		BufferedReader error = new BufferedReader(new InputStreamReader(
				process.getErrorStream()));
		try {
			while ((line = error.readLine()) != null) {
				System.out.println(line);
				if (line.contains("error")) {
					System.out
							.println("Please check Adb Connection or Check USB debug option enabled in Settings->Developer Option ");
					return false;
				}
			}
			error.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void ManageIncompleteTestRun() throws SQLException {
		int ETR = 0;
		try {

			ETR = dbAcess.CheckTestRunStatus();
			if (ETR == 1 || ETR == 6) {
				ff = new Thread(DownloadAndLaunchThread);
				ff.start();
			} else if (ETR == 2 || ETR == 3) {
				System.out.println("Got the Status as 3");
				GetHostDeviceData();
				Thread.sleep(10000);
				ExitDeviceAgent();
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			ExitDeviceAgent();
		}
	}

	private void GetHostDeviceData() throws SQLException {
		try {
			Thread.sleep(2000);
			dbAcess.GetFTPDetails();
		} catch (Exception e1) {
			e1.printStackTrace();
			HostDeviceDataNotAvailable = true;
			ExitDeviceAgent();
		}
	}

	Runnable DownloadAndLaunchThread = new Runnable() {
		@Override
		public void run() {
			downloadTestCase();
		}
	};

	public void ExitDeviceAgent() {
		try {
			if (!HasAgentExited) {
				HasAgentExited = true;
				timer2.cancel();
				dB_Timer_MonitorExit = null;
				dB_Timer_MonitorStart = null;

				WaitForShutDownStop = true;
				dbAcess = null;
				System.exit(0);
			}
		} catch (final Exception ex) {
			publishProgress(ex.toString());
		}
	}

	protected void downloadTestCase() {
		System.out.println("Download and launch");
		try {
			GetHostDeviceData();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		DownloadFiles();
		if (FTPDone == true) {
			dbAcess.updateTime("UPDATE HostDevice SET FTPStatus=1 WHERE DeviceMAC='"
					+ deviceId + "'");
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Exiting Device agent successfully");
			ExitDeviceAgent();
		} else {
			if (HostDeviceDataNotAvailable == true) {
				System.out
						.println("Download Files Failed as TestRun Data not available!\r\nPlease warmboot the device and make sure device has a valid IP prior to starting DeviceAgent again!DownloadAndLaunch");
				ExitDeviceAgent();
			} else {
				System.out
						.println("Download Files Failed!\r\nPlease warmboot the device and make sure device has a valid IP prior to starting DeviceAgent again!DownloadAndLaunch");
				ExitDeviceAgent();
			}
		}
	}

	private void ClearDeviceInfo() {
		dbAcess.updateTime("Delete From DeviceInfo WHERE DeviceMAC='"
				+ deviceId + "'");
	}
}
