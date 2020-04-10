package com.example.CalorieCounter;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;

/**
 * MainActivity class
 * @author Elad Massad
 * @author Ilya Vogman
 */

public class MainActivity extends AppCompatActivity {
    /**
     * Database class Model
     */
    class Model{
        private static final String databaseName = "CalorieCounter";
        private static final int databaseVersion = 11;



        private Model.DatabaseHelper DBHelper;
        private SQLiteDatabase db;


        public Model(){

            DBHelper = new Model.DatabaseHelper(MainActivity.this);

        }

        /**
         * DataBase Helper
         */
        private class DatabaseHelper extends SQLiteOpenHelper {
            DatabaseHelper(Context context){
                super(context,databaseName,null,databaseVersion);
            }

            @Override
            public void onCreate(SQLiteDatabase db){
/**
 * creating food table with fields
 *
 */

                try{
                    db.execSQL("CREATE TABLE IF NOT EXISTS food (" +
                            " food_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            " date VARCHAR, " +
                            " foodId VARCHAR, " +
                            " food_name VARCHAR, " +
                            " food_calories VARCHAR);");

                }
                catch (SQLException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion,int newVersion){
                db.execSQL("DROP TABLE IF EXISTS food ");
                onCreate(db);
                String TAG = "Tag";
                Log.w(TAG, "Upgrading database from version " + oldVersion + "to " + newVersion + ", which will destroy all data");
            }

/**
 * opening the database inorder to preform SQLite execution
 * @see DatabaseHelper
 */
        }
        public Model open() throws SQLException{
            db = DBHelper.getWritableDatabase();
            return this;
        }

        /**
         * closing database after preformed SQLite execution
         * @see DatabaseHelper
         */
        public void close(){
            DBHelper.close();
        }
        /**
         *  inserting into the Database
         * @param table
         * @param fields
         * @param values
         */

        public void insert(String table, String fields, String values){
            db.execSQL("INSERT INTO " + table + "(" + fields + ") VALUES (" + values + ");");
        }
        /**
         * getting amount of calories for a specific meal per date using Cursor to iterate
         * @param date
         * @param foodId
         * @param table
         * @param fields
         * @return integer of the sum of calories
         */
        public int get(String date ,String foodId ,String table, String fields){
            Cursor cursor = db.rawQuery("SELECT SUM(" + fields +") "
                    + "FROM " +table +

                    " WHERE (foodId = '"+ foodId+"' AND date = '"+date + "')" +
                    " GROUP BY foodId;",null);
            if(cursor.moveToFirst()){
                return cursor.getInt(0);
            }
            return 0;
        }

        /**
         * getting amount of calories for the entire day on specific date
         * @param date
         * @return integer of the sum of calories
         */
        public int getWholeDay(String date){
            Cursor cursor = db.rawQuery("SELECT SUM(food_calories) " +
                    " FROM food " +
                    " GROUP BY date " +
                    " HAVING date = '" + date +"';",null);
            if(cursor.moveToFirst()){
                return cursor.getInt(0);
            }
            return 0;

        }

        /**
         * deletes specific food from the table
         * @param foodId
         * @param foodName
         * @param date
         */
        public void delete(String foodId,String foodName, String date){
            db.execSQL("DELETE FROM food "+
                    " WHERE ( foodId = '"+ foodId+ "' " +
                    " AND food_name = '" + foodName + "' " +
                    " AND date = '" + date + "' );");
        }

        /**
         * get the entire table for a certain meal
         * @param date
         * @param foodId
         * @return List of Lists of String representing the food name and amount of calories
         */
        public List<List<String>> getTable(String date ,String foodId){

            List<List<String>> rows = new ArrayList<List<String>>();
            Cursor cursor = db.rawQuery(" SELECT food_name, food_calories FROM food " +
                    " WHERE ( foodId = '" + foodId +"' AND date = '"+ date+"');",null);
            if(!cursor.isAfterLast()&& cursor.moveToFirst())do {
                int foodnameIndex = cursor.getColumnIndex("food_name");
                String row =cursor.getString(foodnameIndex);
                Log.d("food_name",cursor.getString(cursor.getColumnIndex("food_name")));
                int columnIndex =cursor.getColumnIndex("food_calories");
                String columnValue = cursor.getString(columnIndex);
                Log.d("columnValue", "getTable: "+columnValue);
                String row1 =columnValue;
                List<String> list = new ArrayList();
                list.add(row);
                list.add(row1);
                rows.add(list);
            }while (cursor.moveToNext());
            Log.d("length","rows.size()= " +rows.size());
            cursor.close();
            return rows;
        }

        /**
         * get the food table for an entire day
         * @param date
         * @return List of Lists of String representing food name and amount of calories
         */
        public List<List<String>> getWholeTable(String date){
            List<List<String>> rows = new ArrayList<List<String>>();
            Cursor cursor = db.rawQuery(" SELECT food_name, food_calories FROM food " +
                    " WHERE ( date = '" + date + "')" +
                    " ORDER By foodId;",null);
            Log.d("cooshomo","fuck");
            if(!cursor.isAfterLast()&& cursor.moveToFirst())do {
                int foodnameIndex = cursor.getColumnIndex("food_name");
                String row =cursor.getString(foodnameIndex);
                Log.d("food_name",cursor.getString(cursor.getColumnIndex("food_name")));
                int columnIndex =cursor.getColumnIndex("food_calories");
                String columnValue = cursor.getString(columnIndex);
                Log.d("columnValue", "getTable: "+columnValue);
                String row1 =columnValue;
                List<String> list = new ArrayList();
                list.add(row);
                list.add(row1);
                rows.add(list);
            }while (cursor.moveToNext());
            Log.d("length","rows.size()= " +rows.size());
            cursor.close();
            return rows;

        }
    }

    /**
     * ViewModel class
     */
    class ViewModel{

        WebView webView;
        Model model;


        public ViewModel(WebView webView, Model model) {
            this.webView = webView;
            this.model = model;

        }
        /**
         * inserting food into Database using Javascript interface
         * @param date
         * @param foodId
         * @param foodNameInput
         * @param calAmountInput
         */
        @android.webkit.JavascriptInterface
        public void inputData(String date,String foodId ,String foodNameInput,String calAmountInput){

            Log.d("myMsg","date: " + date + "foodId: "+foodId + "food name: " + foodNameInput + ", amount in calories: " + calAmountInput);

            model.open();
/**
 * using model.insert()
 * @see Model
 */
            model.insert("food", "food_id, date, foodId, food_name, food_calories", "NULL, '"+date+"', "+"'"+ foodId +"', '" + foodNameInput + "', '" + calAmountInput + "'");

            model.close();
            Log.d("food table","food added to the list");

        }
        /**
         * getting amount of calories for a specific meal on a specific date using Javascript Interface
         * @param date
         * @param foodId
         * @return sum of calories for specific day and meal
         */
        @android.webkit.JavascriptInterface
        public int getAmount(String date ,String foodId){
            model.open();
            int sum = model.get(date,foodId,"food","food_calories");
            model.close();
            return sum;

        }
        /**
         * getting amount of calories for entire day on a specific date
         * @param date
         * @return sum of the calories for entire day
         * @see Model
         */
        @android.webkit.JavascriptInterface
        public int getWholeDay(String date){
            model.open();
            int sum = model.getWholeDay(date);
            model.close();
            return sum;
        }

        /**
         * deletes the desired food from the table
         * using model.delete()
         * @param foodId
         * @param foodName
         * @param date
         * @see Model
         */
        @android.webkit.JavascriptInterface
        public void delete(String foodId, String foodName, String date){
            model.open();
            model.delete(foodId,foodName,date);
            model.close();
        }

        /**
         * getting the food table for meal on a specific date
         * receives List of List of String from the Model
         * and pushes the data into a JSON Array and then toString
         * @param date
         * @param foodId
         * @return String in JSON format to the javascriptInterface
         * @see Model
         */
        @android.webkit.JavascriptInterface
        public String getTable(String date, String foodId){
            model.open();
            List<List<String>> list = model.getTable(date, foodId);
            JSONArray jsonArray =new JSONArray();
            for(int i=0;i<list.size();i++){
                JSONArray jrr = new JSONArray();
                for(int j = 0;j < list.get(i).size() ; j++){

                    jrr.put(list.get(i).get(j));
                    System.out.println(list.get(i).get(j));
                }
                jsonArray.put(jrr);
            }
            model.close();
            return jsonArray.toString();
        }

        /**
         * getting the food table for an entire day
         * receives List of List of String from the model
         * and pushes the data into a JSON Array and then toString
         * @param date
         * @return String in JSON format
         * @see Model
         */
        @android.webkit.JavascriptInterface
        public String getWholeTable(String date){
            model.open();
            List<List<String>> list = model.getWholeTable(date);
            JSONArray jsonArray =new JSONArray();
            for(int i=0;i<list.size();i++){
                JSONArray jrr = new JSONArray();
                for(int j = 0;j < list.get(i).size() ; j++){

                    jrr.put(list.get(i).get(j));
                    System.out.println(list.get(i).get(j));
                }
                jsonArray.put(jrr);
            }

            model.close();

            return jsonArray.toString();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**
         *  using Stetho to view the Database
         */
        Stetho.initializeWithDefaults(this);

        new OkHttpClient.Builder().addNetworkInterceptor(new StethoInterceptor()).build();
        /**
         *  creating WebView
         * @see WebView
         */
        final WebView webView = new WebView(this);
        /**
         * creating Model
         * @see Model
        */
        final Model model = new Model();
        /**
         *  creating ViewModel
         * @see ViewModel
         */
        final ViewModel vm = new ViewModel(webView,model);

        webView.getSettings().setJavaScriptEnabled(true);
        /**
         *  adding ViewModel to the Javascript Interface
         */
        webView.addJavascriptInterface(vm,"vm");

        /**
         *  loading my HTML file to the WebView
         */
        webView.loadUrl("file:///android_asset/home.html");

        setContentView(webView);

    }

}

