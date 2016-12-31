package geekband.yanjinyi1987.com.bluetoothcomm.Database;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lexkde on 16-12-31.
 */

public class DBService extends Service{
    //对数据库的操作是需要放在额外的线程中的，但是每次查询就开启一个新线程也不太好额
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public class LocalBinder extends Binder {
        public DBService getService() {
            return DBService.this;
        }
    }
}

class DBHelper extends SQLiteOpenHelper {
    public DBHelper(Context context) {
        super(context, CityListOperations.DB_NAME,null,1);
    }

    private static DBHelper mInstance; //single instance
    public synchronized  static DBHelper getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new DBHelper(context);
        }
        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("create table " +
                    CityListOperations.TABLE_HE_XUN_CITY_LIST +
                    "(" +
                    CityListOperations.SECTION_CITY + " varchar(20) not null, " +
                    CityListOperations.SECTION_COUNTRY + " varchar(20) not null, " +
                    CityListOperations.SECTION_CITY_ID + " varchar(20) not null, " +
                    CityListOperations.SECTION_LATITUDE + " varchar(20) not null, " +
                    CityListOperations.SECTION_LONGITUDE + " varchar(20) not null, " +
                    CityListOperations.SECTION_PROVINCE + " varchar(20) not null"+
                    ")"
            );


            db.execSQL("create table " +
                    CityListOperations.TABLE_CHOSEN_CITY_LIST +
                    "(" +
                    CityListOperations.SECTION_CITY + " varchar(20) not null, " +
                    CityListOperations.SECTION_COUNTRY + " varchar(20) not null, " +
                    CityListOperations.SECTION_CITY_ID + " varchar(20) not null, " +
                    CityListOperations.SECTION_LATITUDE + " varchar(20) not null, " +
                    CityListOperations.SECTION_LONGITUDE + " varchar(20) not null, " +
                    CityListOperations.SECTION_PROVINCE + " varchar(20) not null, "+
                    CityListOperations.SECTION_WEATHER_JSON + " text null" +
                    ")"
            );
        }catch (SQLiteException e) {
            e.printStackTrace();
        }
        finally {
            Log.i("SQLite","add table successfully");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}

class CityListOperations {
    public static final String DB_NAME = "IrrigationParameters.db";
    public static final String TABLE_HE_XUN_CITY_LIST = "HeXunCityList";
    public static final String TABLE_CHOSEN_CITY_LIST = "ChoosedCityList";
    public static final String SECTION_CITY = "city";
    public static final String SECTION_COUNTRY = "country";
    public static final String SECTION_CITY_ID = "cityId";
    public static final String SECTION_LATITUDE = "latitude";
    public static final String SECTION_LONGITUDE = "longitude";
    public static final String SECTION_PROVINCE = "province";
    public static final String SECTION_WEATHER_JSON = "weather_json";

    private DBHelper helper;
    private SQLiteDatabase db;

    Context mContext;

    public CityListOperations(Context context) {
        helper = DBHelper.getInstance(context);
        db = helper.getWritableDatabase();
        mContext = context;
    }

    public long insert_to_table_city(SQLiteDatabase db, String table_name, WeatherService.CityInfo city) {
        //实例化常量值
        ContentValues cValue = new ContentValues();
        //添加用户名
        cValue.put(SECTION_CITY, city.getCity());
        //添加密码
        cValue.put(SECTION_COUNTRY, city.getCnty());
        //
        cValue.put(SECTION_CITY_ID, city.getId());
        //
        cValue.put(SECTION_LATITUDE, city.getLat());
        //
        cValue.put(SECTION_LONGITUDE, city.getLon());
        //
        cValue.put(SECTION_PROVINCE, city.getProv());

        //调用insert()方法插入数据
        return db.insert(table_name, null, cValue);
    }

    public boolean insert_to_table_cityLists(SQLiteDatabase db, String table_name, List<WeatherService.CityInfo> cityList) {
        for (WeatherService.CityInfo city : cityList
                ) {
            if(insert_to_table_city(db, table_name, city)==-1) {
                return false;
            }
        }
        return true;
    }

    public boolean saveData(List<WeatherService.CityInfo> cityLists) {
        Log.i(this.getClass().getSimpleName(),"save global city list into DB");
        synchronized (helper) {
            boolean result = false;
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            db.beginTransaction();
            try {
                db.execSQL("DELETE FROM "+TABLE_HE_XUN_CITY_LIST);
                if(insert_to_table_cityLists(db, TABLE_HE_XUN_CITY_LIST, cityLists)) {
                    db.setTransactionSuccessful();
                    result=true;
                }
                else {
                    result = false;
                }
            } catch (SQLiteException e) {
                result = false;
                e.printStackTrace();
            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
                return result;
            }
        }
    }

    public List<WeatherService.CityInfo> sendDatatoMemory() {
        Log.i(this.getClass().getSimpleName(),"read global city list from db");
        //Map<String, WeatherService.ProvinceList> provinceLists = new HashMap<>(); //define a Map
        List<WeatherService.CityInfo> cityInfos = new ArrayList<>();
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            Cursor cursor = db.query(TABLE_HE_XUN_CITY_LIST, null, null, null, null, null, null); //get all rows
            //String currentZXS = null;
            try {
                if (cursor.moveToFirst()) {
                    do {
                        WeatherService.CityInfo cityInfo = new WeatherService.CityInfo(cursor.getString(0),//city
                                cursor.getString(1),//country
                                cursor.getString(2),//cityId
                                cursor.getString(3),//latitude
                                cursor.getString(4),//longitude
                                cursor.getString(5));//province

                        cityInfos.add(cityInfo);

//                        if (isZXS(cityInfo.getCity())) {
//                            currentZXS = cityInfo.getCity();
//                            provinceLists.put(currentZXS, new WeatherService.ProvinceList(currentZXS));
//                        }
//
//                        if (TextUtils.equals("直辖市", cityInfo.getProv()) || TextUtils.equals("特别行政区", cityInfo.getProv())) {
//                            provinceLists.get(currentZXS).getCities().add(cityInfo);
//                        } else {
//                            if (provinceLists.get(cityInfo.getProv()) == null) {
//                                provinceLists.put(cityInfo.getProv(), new WeatherService.ProvinceList(cityInfo.getProv()));
//                            }
//                            provinceLists.get(cityInfo.getProv()).getCities().add(cityInfo);
//                        }
                    } while (cursor.moveToNext());
                } else {
                    Toast.makeText(mContext, "获取地区数据失败!", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
                db.close();
            }
        }
        return cityInfos;
    }

    public List<WeatherService.CityInfo> getChoosedCities() {
        List<WeatherService.CityInfo> cities = new ArrayList<>();
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            Cursor cursor = db.query(TABLE_CHOSEN_CITY_LIST, null, null, null, null, null, null); //get all rows
            try {
                if (cursor.moveToFirst()) {
                    do {
                        WeatherService.CityInfo city = new WeatherService.CityInfo(cursor.getString(0),//city
                                cursor.getString(1),//country
                                cursor.getString(2),//cityId
                                cursor.getString(3),//latitude
                                cursor.getString(4),//longitude
                                cursor.getString(5));//province

                        cities.add(city);
                    } while (cursor.moveToNext());
                } else {
                    Toast.makeText(mContext, "您还没有选择城市!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                cities = null;
            } finally {
                cursor.close();
                db.close();
                return cities;
            }
        }
    }

    public long saveChoosedCity(WeatherService.CityInfo city) {
        long insertResult = -1;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            Cursor cursor = db.query(TABLE_CHOSEN_CITY_LIST,
                    null,
                    SECTION_CITY_ID + "=" + "\"" + city.getId() + "\"", null, null, null, null);
            try {
                db.beginTransaction();
                if (cursor.moveToFirst() == false) {
                    insertResult = insert_to_table_city(db, TABLE_CHOSEN_CITY_LIST, city);
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                cursor.close();
                db.close();
            }
        }
        return insertResult;
    }

    public int deleteChoosedCity(String cityId) {
        int result = 0;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            try {
                db.beginTransaction();
                result = db.delete(TABLE_CHOSEN_CITY_LIST, SECTION_CITY_ID + "=" + "\"" + cityId + "\"", null);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        }
        return result;
    }

    public long cacheWeathers(ArrayList<String> weatherJsons) {

        long insertResult = -1;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            try {
                db.beginTransaction();
                for (String weatherJson: weatherJsons
                        ) {
                    insertResult=-1;
                    Gson gson = new Gson();
                    HeXunWeatherInfo heXunWeatherInfo = gson.fromJson(weatherJson, HeXunWeatherInfo.class);
                    String cityId = heXunWeatherInfo.heWeatherDS0300.get(0).basic.id;
                    String cityName = heXunWeatherInfo.heWeatherDS0300.get(0).basic.city;
                    Log.i("cacheWeathers",cityName);
                    Cursor cursor = db.query(TABLE_CHOSEN_CITY_LIST,
                            null,
                            SECTION_CITY_ID + "=" + "\"" + cityId + "\"",
                            null, null, null, null);
                    if (!cursor.moveToFirst()) {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(SECTION_WEATHER_JSON, weatherJson);
                        insertResult = db.update(TABLE_CHOSEN_CITY_LIST,
                                contentValues,
                                SECTION_CITY_ID + "=" + "\"" + cityId + "\"",
                                null);
                    }
                    cursor.close();
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
                return insertResult;
            }
        }

    }


    public int syncdelete(String cityId) {
        int result = 0;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            try {
                db.beginTransaction();
                result = db.delete(TABLE_CHOSEN_CITY_LIST, SECTION_CITY_ID + "=" + "\"" + cityId + "\"", null);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
                return result;
            }
        }
    }

    public int updateCachedWeather(String cityId, String weatherJson) {
        //实例化常量值
        int result = 0;
        ContentValues cValue = new ContentValues();
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            cValue.put(SECTION_WEATHER_JSON, weatherJson);
            try {
                db.beginTransaction();
                //调用update()方法插入数据
                result = db.update(TABLE_CHOSEN_CITY_LIST, cValue, SECTION_CITY_ID + "=" + "\"" + cityId + "\"", null);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
                return result;
            }
        }
    }

    public List<String> getCachedWeathers() {
        ArrayList<String> weatherJson = new ArrayList<>();
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            Cursor cursor = db.query(TABLE_CHOSEN_CITY_LIST, null,
                    null,
                    null, null, null, null); //get all rows
            try {
                if (cursor.moveToFirst()) {
                    do {
                        weatherJson.add(cursor.getString(6));
                    }while(cursor.moveToNext());
                } else {
                    //Toast.makeText(mContext,"您还没有选择城市!",Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
                db.close();
                return weatherJson;
            }
        }
    }
}
