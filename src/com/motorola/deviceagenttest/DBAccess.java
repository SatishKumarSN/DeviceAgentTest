package com.motorola.deviceagenttest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBAccess {

	String DeviceMAC, DeviceIP, DeviceID;

	private java.sql.Connection myConn;
	public String DBIP;
	public String DBUser;
	public String DBPass;
	public int dbType;
	public String sConnection = "";
	public boolean databaseOpen = false;
	public String ftpIP, ftpUserName, ftpPswd, ExeName;

	public enum sqlResult {
		Database_Opened, Database_Opened_Failed, Table_Created, Table_Exist, Table_Creation_Failed, Insert_Success, Insert_Failed, Delete_Success, Delete_Failed, Database_Closed, Database_Closed_Failed, WriteLogFile_Success, WriteLogFile_Failed

	}

	public String OpenConnection() {
		System.out.println("Open Connection");

		try {
			sConnection = "jdbc:jtds:sqlserver://" + DBIP + ":1433/EMSTAF";
			System.out.println("Trying to to connection with DriverMaanger ");
			myConn = java.sql.DriverManager.getConnection(sConnection, DBUser,
					DBPass);
			databaseOpen = true;
			System.out.println("Data connection pass");

			return "Database_Opened";
		}

		catch (SQLException e) {
			System.out.println("Open Connection Failed" + e.getMessage());
			e.printStackTrace();
			return e.getMessage();
		}

		catch (Exception e) {
			System.out.println("Open Connection Failed" + e.getMessage());
			e.printStackTrace();
			return e.getMessage();
		}
	}

	public boolean CheckConnection() {
		boolean var = false;
		try {
			System.out.println("Check Connection");
			if (OpenConnection() == "Database_Opened") {
				myConn.close();
				var = true;
			} else {
				var = false;
			}
		} catch (Exception ex) {
			var = false;
		}

		return var;
	}

	private String InsertRecord(String strSQL) {
		System.out.println("Insert Record");
		try {
			if (OpenConnection() == "Database_Opened") {
				Statement statement = myConn.createStatement();
				statement.executeUpdate(strSQL);
				CloseConnection();
				return "Insert_Success";
			} else {
				return "OpenConnection failed";
			}

		} catch (SQLException e2) {
			return "Insert_Failed: " + e2.getMessage().toString();
		} catch (Exception e2) {
			return "Insert_Failed: " + e2.getMessage().toString();
		}
	}

	public String updateTime(String strSQL) {
		String iInsert;
		iInsert = strSQL;
		String i = InsertRecord(iInsert);
		if (i == "Insert_Success") {
			return "Insert_Success";
		} else {
			return i;
		}
	}

	public String InsertDeviceInfo(String deviceIp, String deviceId, String deviceName, int status) {
		System.out.println("InsertDeviceInfo");
		try {
			if (OpenConnection() == "Database_Opened") {

				String sql = "DELETE FROM DeviceInfo WHERE DeviceMAC='"
						+ deviceId + "'";
				Statement statement = myConn.createStatement();
				statement.executeUpdate(sql);
				sql = "INSERT INTO DeviceInfo(DeviceIP, DeviceMAC, DeviceName, Status) VALUES('"
						+ deviceIp
						+ "','"
						+ deviceId
						+ "','"
						+ deviceName + "'," + status + ")";
				statement.executeUpdate(sql);
				CloseConnection();
				System.out.println("Inserted the Device Id " + deviceId);
				return "Insert_Success";
			} else {
				System.out.println("DataBase connection fails");
				return "OpenConnection Failed";
			}
		} catch (Exception t1) {
			System.out.println(t1.getMessage().toString());
			return "Exception: " + t1.getMessage().toString();
		}
	}

	public String updateDeviceStatus(String deviceId, String TestStatus) {
		System.out.println("updateDeviceStatus");
		try {
			if (OpenConnection() == "Database_Opened") {

				String sql = "UPDATE HostDevice SET Status='" + TestStatus
						+ "' WHERE DeviceMAC='" + deviceId + "'";
				Statement statement = myConn.createStatement();
				statement.executeUpdate(sql);
				CloseConnection();
				return "Update_Success";
			} else {
				return "OpenConnection failed";
			}
		} catch (Exception t1) {
			return "Exception: " + t1.getMessage().toString();
		}
	}

	public boolean checkDeviceIdTableExistenceInDB(String deviceId) {
		try {
			if (OpenConnection() == "Database_Opened") {

				System.out.println("Iny checkDeviceIdTableExistenceInDB "
						+ deviceId);
				String sql = "SELECT * FROM DeviceInfo WHERE DeviceMAC='"
						+ deviceId + "'";
				Statement statement = myConn.createStatement();
				ResultSet results = statement.executeQuery(sql);
				System.out.println("results " + results.toString());

				while (results.next()) {
					return true;
				}
				CloseConnection();
				System.out
						.println("Device Id is not exist in Device Info Table");
				return false;
			} else {
				System.out.println("Data Base open fails ");
				return false;
			}
		} catch (Exception t1) {
			System.out.println(t1.toString());
			return false;
		}
	}

	public boolean CheckDeviceIdEntryExistenceforHostDeviceTable(String deviceId) {
		try {
			if (OpenConnection() == "Database_Opened") {

				System.out.println("Iny CheckDeviceIdEntryExistenceforHostDeviceTable "
						+ deviceId);
				String sql = "SELECT * FROM HostDevice WHERE DeviceMAC='"
						+ deviceId + "'";
				Statement statement = myConn.createStatement();
				ResultSet results = statement.executeQuery(sql);
				System.out.println("results " + results.toString());
				while (results.next()) {
					return true;
				}
				CloseConnection();
				return false;
			} else {
				System.out.println("Data Base open fails ");
				return false;
			}
		} catch (Exception t1) {
			t1.printStackTrace();
			return false;
		}
	}

	public void setDeviceMAC(String MAC) {
		try {
			DeviceMAC = MAC;

		} catch (Exception t1) {
			t1.printStackTrace();

		}
	}

	public int CheckTestRunStatus() {
		try {
			int ret = 0;
			if (OpenConnection() == "Database_Opened") {
				String TestRunStatus = null;
				System.out.println("Device ID Value in CheckTestRumStatus " + DeviceID);
				String sql = "SELECT ExitTestRun FROM HostDevice WHERE DeviceMAC='"
						+ DeviceID + "'";
				Statement statement = myConn.createStatement();
				ResultSet results = statement.executeQuery(sql);

				while (results.next()) {
					TestRunStatus = results.getString("ExitTestRun");
					ret = Integer.parseInt(TestRunStatus);
				}
				CloseConnection();
				System.out.println("ExitTestRun Status " + ret);
				return ret;
			} else {
				return 0;
			}
		} catch (Exception t1) {
			t1.printStackTrace();
			return 0;
		}
	}

	public int GetFTPDetails() {
		try {
			if (OpenConnection() == "Database_Opened") {
				String TestRunStatus = null;
				String sql = "SELECT * FROM HostDevice WHERE DeviceMAC='"
						+ DeviceID + "'";
				Statement statement = myConn.createStatement();
				ResultSet results = statement.executeQuery(sql);

				while (results.next()) {
					TestRunStatus = results.getString("ExitTestRun");
					ftpIP = results.getString("FTPIPAddr");
					ftpUserName = results.getString("FTPUser");
					ftpPswd = results.getString("FTPPass");
					ExeName = results.getString("TestProgram");
					ExeName = "Prd32255.jar";
					System.out.println(TestRunStatus);
				}
				CloseConnection();
				return Integer.parseInt(TestRunStatus);
			} else {
				return 0;
			}
		} catch (Exception t1) {
			t1.printStackTrace();
			return 0;
		}
	}

	public ResultSet getDataSet(String SQL) {
		// System.out.println("Get data set");
		ResultSet data = null;

		try {
			String tSelect;
			tSelect = SQL;
			if (OpenConnection() == "Database_Opened") {

				Statement statement = myConn.createStatement();
				data = statement.executeQuery(tSelect);
				CloseConnection();

				return data;
			} else {
				return data;
			}
		} catch (SQLException e3) {

		} catch (Exception e3) {

		} finally {

		}
		return data;
	}

	public String RemoveDeviceInfo(String DeviceMAC) {
		try {

			if (OpenConnection() == "Database_Opened") {

				String sql = "DELETE FROM DeviceInfo WHERE DeviceMAC='"
						+ DeviceID + "'";
				Statement statement = myConn.createStatement();
				statement.executeUpdate(sql);
				CloseConnection();
				return "Remove_Success";
			} else {
				return "OpenConnection falied";
			}
		} catch (Exception t1) {
			return "Exception: " + t1.getMessage().toString();
		}
	}

	public sqlResult CloseConnection() {
		System.out.println("Close connection");
		try {
			myConn.close();
			return sqlResult.Database_Closed;
		} catch (SQLException e5) {
			return sqlResult.Database_Closed_Failed;
		} catch (Exception e5) {
			return sqlResult.Database_Closed_Failed;
		}
	}

}
