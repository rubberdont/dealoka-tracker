package codemagnus.com.dealokav2.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import codemagnus.com.dealokav2.tower.TowerRequest;
import codemagnus.com.dealokav2.utils.GeneralUtils;

public class RequestDataSource {

	private static final String TAG  = "UserDataSource";
	private SQLiteDatabase database;
	private RequestDBHelper dbHelper;

    private String[] columns = {
        RequestDBHelper.KEY_ID,
        RequestDBHelper.KEY_POST,
        RequestDBHelper.KEY_STATUS,
        RequestDBHelper.KEY_DATE
    };

	public RequestDataSource(Context context) {
		dbHelper = new RequestDBHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public boolean isOpen(){
		if(database.isOpen())
			return true;
		
		return false;
	}
	
	public boolean chkIfRequestDBIsEmpty() {
		if(dbHelper == null){
			return true;
		}
		if(database == null){
			return true;
		}
		Cursor mCursor = database.rawQuery("SELECT * FROM " + RequestDBHelper.TABLE_POST, null);
		Boolean rowExists;

		if (mCursor.moveToFirst()){
		   // DO SOMETHING WITH CURSOR
		  rowExists = false;
		} else {
		   // I AM EMPTY
		   rowExists = true;
		}
		return rowExists;
	}

	public TowerRequest getRequestById(String Id) {
        TowerRequest user = null;
		String sql = DBHelper.TAG_SELECT + " " + RequestDBHelper.TABLE_POST +" " + DBHelper.TAG_WHERE + ""
				+ RequestDBHelper.KEY_ID + "= '" + Id + "'";
		Cursor c = database.rawQuery(sql, null);
		
		if(c != null && c.getCount() > 0){
			c.moveToFirst();
			user = cursorToTower(c);
		}
		
		return user;
	}

    public List<TowerRequest> getAllRequests() {
        List<TowerRequest> requests = new ArrayList<>();

        Cursor cursor = database.query(RequestDBHelper.TABLE_POST, columns, null, null, null, null, null);
        if(cursor != null && cursor.getCount() > 0){
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                TowerRequest request = cursorToTower(cursor);
                if(requests != null) {
                    requests.add(request);
                }
                cursor.moveToNext();
            }
        }

        return requests;
    }

	public TowerRequest createRequest(TowerRequest tower) {
		ContentValues values = new ContentValues();

        values.put(RequestDBHelper.KEY_POST, 	tower.getPost().toString());
        values.put(RequestDBHelper.KEY_STATUS, 	tower.getStatus());
        values.put(RequestDBHelper.KEY_DATE,    GeneralUtils.getCurrentDate());

		long insertId = database.insert(RequestDBHelper.TABLE_POST, null, values);
		Cursor cursor = database.query(RequestDBHelper.TABLE_POST, columns, RequestDBHelper.KEY_ID + " = " +
		insertId, null, null, null, null);
		cursor.moveToFirst();

        TowerRequest twr = cursorToTower(cursor);
		cursor.close();

		return twr;
	}
	
	public void deleteOldData() {
        Cursor cursor = database.query(RequestDBHelper.TABLE_POST, columns, null, null, null, null, null);
        cursor.moveToFirst();

        TowerRequest tower = cursorToTower(cursor);
		database.delete(RequestDBHelper.TABLE_POST, RequestDBHelper.KEY_ID + " = '" + tower.getId() + "'", null);
	}


	private TowerRequest cursorToTower(Cursor cursor) {
        TowerRequest tower = null;
        try {
            tower = new TowerRequest(new JSONObject(cursor.getString(1)));
            tower.setId(cursor.getString(0));
            tower.setStatus(cursor.getString(2));
            tower.setDate(cursor.getString(3));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return tower;
	}
}
