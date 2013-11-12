package com.motorola.deviceagenttest;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.SwingWorker;

@SuppressWarnings("serial")
public class DeviceAgentTest extends JFrame implements ActionListener {

	ArrayList<String> labels = new ArrayList<String>();
	private JLabel serverIpLabel;
	private JLabel userNameLabel;
	private JLabel passwordLabel;
	private JLabel serverTypeLabel;

	// Strings for the labels
	private String SERVER_IP = "Server Ip";
	private String USER_NAME = "UserName";
	private String PASSWORD = "Password";
	private String SERVER_TYPE = "ServerType";

	private JTextField serverIpField;
	private JTextField userNameField;
	private JTextField passwordField;
	private JSpinner spinner;

	private DBAccess dbAccess = new DBAccess();
	DeviceAgent deviceAgent = new DeviceAgent();

	public DeviceAgentTest() {

		JPanel panel = new JPanel();
		panel.setBackground(Color.LIGHT_GRAY);
		final JFrame frame = new JFrame("DeviceAgent");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		panel.setLayout(null);

		frame.getContentPane().add(panel);

		serverIpLabel = new JLabel(SERVER_IP);
		serverIpLabel.setBounds(81, 65, 64, 14);
		panel.add(serverIpLabel);

		serverIpField = new JTextField(20);
		serverIpField.setBounds(81, 79, 166, 20);
		serverIpField.addActionListener(this);
		panel.add(serverIpField);

		userNameLabel = new JLabel(USER_NAME);
		userNameLabel.setBounds(81, 110, 64, 14);
		panel.add(userNameLabel);

		userNameField = new JTextField(20);
		userNameField.setBounds(81, 125, 166, 20);
		userNameField.addActionListener(this);
		panel.add(userNameField);

		passwordLabel = new JLabel(PASSWORD);
		passwordLabel.setBounds(81, 156, 64, 14);
		panel.add(passwordLabel);

		passwordField = new JTextField(20);
		passwordField.setBounds(81, 169, 166, 20);
		passwordField.addActionListener(this);
		panel.add(passwordField);

		JButton saveBtn = new JButton("SAVE");
		saveBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println(e.toString());
				saveConnectionInfo();
			}
		});
		saveBtn.setBounds(81, 218, 81, 35);
		panel.add(saveBtn);

		JButton exitBtn = new JButton("EXIT");
		exitBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println(e.getActionCommand());
				setVisible(false);
				frame.dispose();
			}
		});
		exitBtn.setBounds(172, 218, 75, 35);
		panel.add(exitBtn);

		labels.add("SQL Server 2000");
		labels.add("SQL Express");
		labels.add("SQL Server 2005");

		spinner = new JSpinner(new SpinnerListModel(labels));
		spinner.setBounds(81, 36, 155, 20);
		panel.add(spinner);
		// Tweak the spinner's formatted text field.
		JFormattedTextField ftf = getTextField(spinner);
		if (ftf != null) {
			ftf.setColumns(8); // specify more width than we need
			ftf.setHorizontalAlignment(JTextField.LEFT);
		}
		serverTypeLabel = new JLabel(SERVER_TYPE);
		serverTypeLabel.setBounds(81, 21, 89, 14);
		panel.add(serverTypeLabel);

		frame.setSize(400, 400);
		frame.setVisible(true);
	}

	protected void saveConnectionInfo() {

		dbAccess.DBIP = serverIpField.getText();
		dbAccess.DBUser = userNameField.getText();
		dbAccess.DBPass = passwordField.getText();
		JFormattedTextField dbTypeSelection = getTextField(spinner);
		if (dbTypeSelection != null) {
			System.out.println(dbTypeSelection.getValue());
			System.out.println(labels.indexOf(dbTypeSelection.getValue()));
			dbAccess.dbType = labels.indexOf(dbTypeSelection.getValue());
		} else {
			System.out.println("Some Thing wron in DeviceAgentTest...");
			deviceAgent.publishProgress("Wrong Settings in DBAgentIno  UI");
		}

		new CheckConnectionTaskDBInfo().execute();

	}

	private void storeIpAddress() {
		try {
			File file = new File("D:\\IP_PATH\\", "IPAddr.txt");
			FileOutputStream f = null;
			try {
				f = new FileOutputStream(file, false);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			PrintStream p = new PrintStream(f);
			p.println(serverIpField.getText());
			p.println(userNameField.getText());
			p.println(passwordField.getText());
			p.println(dbAccess.dbType);
			p.close();
			try {
				f.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e1) {
			System.out.println(e1.toString());
		}
	}

	public JFormattedTextField getTextField(JSpinner spinner) {
		JComponent editor = spinner.getEditor();
		if (editor instanceof JSpinner.DefaultEditor) {
			return ((JSpinner.DefaultEditor) editor).getTextField();
		} else {
			System.err.println("Unexpected editor type: "
					+ spinner.getEditor().getClass()
					+ " isn't a descendant of DefaultEditor");
			return null;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println(e.toString());
	}

	class CheckConnectionTaskDBInfo extends SwingWorker<String, String> {
		protected String doInBackground() throws Exception {
			if (!(dbAccess.CheckConnection())) {
				deviceAgent
						.publishProgress("Database credentails are incorrect");
				System.out.println("Credentials incorrect");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.exit(0);

			} else {
				System.out.println("Data Base connection sucess..");
				storeIpAddress();
				System.exit(0);
			}
			return PASSWORD;
		}

		protected void done() {
		}
	}

}
