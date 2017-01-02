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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lexkde on 16-12-31.
 * 不可以直接更新UI。
 虽然Service也是在主线程工作。但是其无法直接更改ui。
 间接的方法有很多的，可以参考android跨进程通信。
 activity绑定Service
 handler.sentMessage()
 handler.post(new Runnable(){})
 BroadcastReceiver
 异步通信机制

 作者：小小哲
 链接：https://www.zhihu.com/question/24109592/answer/88173757
 来源：知乎
 著作权归作者所有，转载请联系作者获得授权。
 */

public class DBService extends Service{
    //对数据库的操作是需要放在额外的线程中的，但是每次查询就开启一个新线程也不太好额
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i("DBService","onBind");
        return new LocalBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("DBService","onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i("DBService","onRebind");
        super.onRebind(intent);
    }

    @Override
    public void onCreate() {
        Log.i("DBService","onCreate");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.i("DBService","onDestroy");
        super.onDestroy();
    }

    @Override
    public void onTrimMemory(int level) {
        Log.i("DBService","onTrimMemory");
        super.onTrimMemory(level);
    }

    @Override
    public void onLowMemory() {
        Log.i("DBService","onLowMemory");
        super.onLowMemory();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("DBService","onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    public class LocalBinder extends Binder {
        public DBService getService() {
            Log.i("DBService","getService");
            return DBService.this;
        }
    }

    //function body
    //测试植物是否存在
    public boolean checkPlantExist(String plantName) {
        PlantDBOperations mPlantDBOperations = new PlantDBOperations(DBService.this);
        return mPlantDBOperations.checkItemExist(plantName);
    }

    //新建植物
    public boolean createNewPlant(String plantName) {
        PlantDBOperations mPlantDBOperations = new PlantDBOperations(DBService.this);
        return mPlantDBOperations.createNewItem(plantName);
    }

    //获取数据库中的所有植物的名称
    public List<String> getPlantsList() {
        PlantDBOperations mPlantDBOperations = new PlantDBOperations(DBService.this);
        return mPlantDBOperations.getPlantsList();
    }

    //读取植物灌溉参数
    public List<String> getIrrigationParameters(String tableName,int growthTimeId) {
        PlantDBOperations mPlantDBOperations = new PlantDBOperations(DBService.this);
        return mPlantDBOperations.getParameters(tableName,growthTimeId);
    }

    //新建或更改灌溉参数
    public boolean insertIrrigationParameters(String tableName,int growthTimeId, List<String> parameters) {
        PlantDBOperations mPlantDBOperations = new PlantDBOperations(DBService.this);
        return mPlantDBOperations.insertParameters(tableName,growthTimeId,parameters);
    }
}

//估计每个植物是个table，然后section包括月份-主键；参数为sections
class DBHelper extends SQLiteOpenHelper {
    public DBHelper(Context context) {
        super(context, PlantDBOperations.DB_NAME,null,1);
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
        db.execSQL("create table " +
                PlantDBOperations.TABLE_PLANT +
                " (" +
                PlantDBOperations.SECTION_PLANT_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+
                PlantDBOperations.SECTION_PLANT_NAME + " varchar(20) not null"+
                ")"
        );

        db.execSQL("create table " +
                PlantDBOperations.TABLE_PARAMETERS +
                " (" +
                PlantDBOperations.SECTION_PLANT_ID+" int not null, "+
                PlantDBOperations.SECTION_GROWTH_TIME_ID + " int not null, "+
                PlantDBOperations.SECTION_INITIAL_VALUE + " varchar(20) null, " +
                PlantDBOperations.SECTION_TEMP_COEFFICIENT + " varchar(20) null, " +
                PlantDBOperations.SECTION_TEMP_OFFSET + " varchar(20) null, " +
                PlantDBOperations.SECTION_HUMIDITY_COEFFICIENT + " varchar(20) null, " +
                PlantDBOperations.SECTION_SUNLIGHT_INTENSITY + " varchar(20) null" +
                ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}

class PlantDBOperations {
    //需要的方法
    /*
    * 1 更新表项参数
    * 2 新建表项
    * 3 获取数据库中的所有表的名称
    * 4 读取表项参数
    * 5 测试表是否存在 - check tableName
    *
    * 鉴于不能创建3维表或者动态的创建表，那么应该应该用外键的方式创建两个二维表来进行整体设计
    * */
    public static final String DB_NAME = "IrrigationParameters.db";
    public static final String TABLE_PLANT = "plants";
    public static final String TABLE_PARAMETERS = "parameters";
    public static final String SECTION_PLANT_NAME = "PlantName";
    public static final String SECTION_PLANT_ID = "PlantId";
    public static final String SECTION_GROWTH_TIME_ID = "GrowthTimeId";
    public static final String SECTION_INITIAL_VALUE = "InitialValue";
    public static final String SECTION_TEMP_COEFFICIENT = "TempCoefficient";
    public static final String SECTION_TEMP_OFFSET = "TempOffset";
    public static final String SECTION_HUMIDITY_COEFFICIENT = "HumidityCoefficient";
    public static final String SECTION_SUNLIGHT_INTENSITY = "SunlightIntensity";

    private DBHelper helper;
    private SQLiteDatabase db;

    Context mContext;

    public PlantDBOperations(Context context) {
        helper = DBHelper.getInstance(context);
        db = helper.getWritableDatabase();
        mContext = context;
    }

    public boolean checkItemExist(String tableName) {
        synchronized (helper) {
            boolean result = false;
            Cursor cursor = null;
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }

            try {
                db.beginTransaction();
                ArrayList<String> columns = new ArrayList<>();
                columns.add(SECTION_PLANT_NAME);
                //cursor = db.query(TABLE_PLANT,(String[])(columns.toArray()),null,null,null,null,null);
                cursor = db.query(TABLE_PLANT,
                        null,
                        SECTION_PLANT_NAME+" = "+"\""+tableName+"\"",
                        null, null, null, null);
                if (cursor.moveToFirst()) {
                    result = true;
                } else {
                    result = false;
                }
            } catch (SQLiteException e) {
                result = false;
                e.printStackTrace();
            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            } finally {
                if(cursor!=null) {
                    cursor.close();
                }
                db.endTransaction();
                db.close();
                return result;
            }
        }
    }

    public boolean createNewItem(String item_name) {
        Log.i(this.getClass().getSimpleName(),"create table "+item_name+" from db");
        List<String> parameterList = null;
        boolean result = false;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            try {
                db.beginTransaction();
                ContentValues cValue = new ContentValues();
                cValue.put(SECTION_PLANT_NAME,item_name);
                db.insert(TABLE_PLANT,null,cValue);
                result = true;
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
    public long insert_to_table_parameters(SQLiteDatabase db,
                                           String table_name,
                                           int plantId,
                                           int growthTimeId,
                                           List<String> parameters) {
        //实例化常量值
        ContentValues cValue = new ContentValues();
        cValue.put(SECTION_PLANT_ID,plantId);
        //
        cValue.put(SECTION_GROWTH_TIME_ID,growthTimeId);
        //
        cValue.put(SECTION_INITIAL_VALUE, parameters.get(0));
        //
        cValue.put(SECTION_TEMP_COEFFICIENT, parameters.get(1));
        //
        cValue.put(SECTION_TEMP_OFFSET, parameters.get(2));
        //
        cValue.put(SECTION_HUMIDITY_COEFFICIENT, parameters.get(3));
        //
        cValue.put(SECTION_SUNLIGHT_INTENSITY, parameters.get(4));

        //调用insert()方法插入数据
        return db.insert(table_name, null, cValue);
    }

    public int getPlantID(String itemName) {
        Cursor cursor = db.query(TABLE_PLANT,
                null,
                SECTION_PLANT_NAME+" = "+"\""+itemName+"\"",
                null, null, null, null);
        int plantID=-1;
        if(cursor.moveToFirst()) {
            plantID = cursor.getInt(0);
        }
        cursor.close();
        return plantID;
    }

    public boolean insertParameters(String itemName,int growthTimeId, List<String> parameters) {
        Log.i(this.getClass().getSimpleName(),"save parameters into DB "+itemName+"\t"+growthTimeId);
        long insertResult = -1;
        Cursor cursor = null;
        synchronized (helper) {
            boolean result = false;
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }

            try {
                db.beginTransaction();
                int plantID = getPlantID(itemName);
                if(plantID!=-1) {
                    cursor = db.query(TABLE_PARAMETERS,
                            null,
                            SECTION_GROWTH_TIME_ID + "=" + growthTimeId +" AND "+SECTION_PLANT_ID + "=" + plantID,
                            null, null, null, null);

                    if (cursor.moveToFirst()) {
                        //使用update
                        ContentValues cValue = new ContentValues();
                        //
                        cValue.put(SECTION_INITIAL_VALUE, parameters.get(0));
                        //
                        cValue.put(SECTION_TEMP_COEFFICIENT, parameters.get(1));
                        //
                        cValue.put(SECTION_TEMP_OFFSET, parameters.get(2));
                        //
                        cValue.put(SECTION_HUMIDITY_COEFFICIENT, parameters.get(3));
                        //
                        cValue.put(SECTION_SUNLIGHT_INTENSITY, parameters.get(4));
                        insertResult = db.update(TABLE_PARAMETERS,
                                cValue,
                                SECTION_GROWTH_TIME_ID + " = " + growthTimeId+" AND "+SECTION_PLANT_ID + "=" + plantID,
                                null);
                        if (insertResult == -1) {
                            result = false;
                        } else {
                            db.setTransactionSuccessful();
                            result = true;
                        }
                    } else {
                        if (insert_to_table_parameters(db, TABLE_PARAMETERS, plantID,growthTimeId, parameters) != -1) {
                            db.setTransactionSuccessful();
                            result = true;
                        } else {
                            result = false;
                        }
                    }
                }
            } catch (SQLiteException e) {
                result = false;
                e.printStackTrace();
            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            } finally {
                if(cursor!=null) {
                    cursor.close();
                }
                db.endTransaction();
                db.close();
                return result;
            }
        }
    }

    public List<String> getParameters(String itemName,int growthTimeId) {
        Log.i(this.getClass().getSimpleName(),"read parameters from db");
        List<String> parameterList = null;
        Cursor cursor = null;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();
            }
            try {
                db.beginTransaction();
                int plantID = getPlantID(itemName);
                cursor = db.rawQuery("SELECT * FROM "+TABLE_PARAMETERS+" WHERE "+SECTION_GROWTH_TIME_ID+" = "+growthTimeId+" AND " +
                        SECTION_PLANT_ID+"="+plantID,null);
                if(cursor.moveToFirst()) {
                    parameterList = new ArrayList<>();
                    parameterList.add(cursor.getString(2));
                    parameterList.add(cursor.getString(3));
                    parameterList.add(cursor.getString(4));
                    parameterList.add(cursor.getString(5));
                    parameterList.add(cursor.getString(6));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(cursor!=null) {
                    cursor.close();
                }
                db.endTransaction();
                db.close();
            }
        }
        return parameterList;
    }

    public List<String> getPlantsList() {
        Log.i(this.getClass().getSimpleName(),"read plants list from db");
        List<String> plantsList = null;
        Cursor cursor = null;
        synchronized (helper) {
            if (!db.isOpen()) {
                db = helper.getWritableDatabase();

            }
            try {
                db.beginTransaction();
                cursor = db.rawQuery("SELECT * FROM " + TABLE_PLANT,null);
                if(cursor.moveToFirst()) {
                    plantsList = new ArrayList<>();
                    do {
                        plantsList.add(cursor.getString(1));
                    }while(cursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(cursor!=null) {
                    cursor.close();
                }
                db.endTransaction();
                db.close();
            }
        }
        return plantsList;
    }
}
