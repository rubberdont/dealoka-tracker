package codemagnus.com.dealokav2.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RequestDBHelper extends SQLiteOpenHelper {

	// SETUP DATABASE
	private static final String DATABASE_NAME 		= "RequestStatusInfos";
	private static final int DATABASE_VERSION 		= DBHelper.VERSION;
	public static final String TABLE_POST			= "post";
	
	// SETUP COLUMNS FOR PRODUCT
	public static final String KEY_ID 				= "_id";
	public static final String KEY_POST 			= "post";
    public static final String KEY_STATUS 			= "status";
    public static final String KEY_DATE 			= "date";

	public RequestDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE "		+ TABLE_POST + " ("
				+ KEY_ID				+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ KEY_POST		        + " TEXT, "
                + KEY_STATUS		    + " TEXT, "
                + KEY_DATE		        + " TEXT "
                + " );");
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_POST);
		onCreate(db);
	}

}
