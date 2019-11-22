package jp.ac.asojuku.accball

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Log.d
import android.view.SurfaceHolder
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() ,
    SensorEventListener , //各種センサーの反応をOSから受け取るためのインターフェース
    SurfaceHolder.Callback//サーフェスViewを実装するための窓口Holderのコールバックインターフェース
    {
        // プロパティ
        private var surfaceWidth:Int = 0; // サーフェスの幅
        private var surfaceHeight:Int = 0; // サーフェスの高さ

        private val radius = 50.0f; // ボールの半径
        private val coef = 1000.0f; // ボールの移動量を計算するための係数（計数）

        private var ballX:Float = 0f; // ボールの現在のX座標
        private var ballY:Float = 0f; // ボールの現在のY座標
        private var vx:Float = 0f; // ボールのX方向の加速度
        private var vy:Float = 0f; // ボールのY方向の加速度
        private var time:Long = 0L; // 前回の取得時間

    //誕生時のライフサイクルイベント
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //リセットボタンで画面をリセットさせる
       reset_button.setOnClickListener{
            result_text.text = "頑張れ！！"
           ballX = 1000f;
           ballY = 50f;
       };
        val holder = surfaceView.holder; // サーフェスホルダーを取得
        // サーフェスホルダーのコールバックに自クラスを追加
        holder.addCallback(this);
        // 画面の縦横指定をアプリから指定してロック(縦方向に指定)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

    }


    override fun onAccuracyChanged(senser: Sensor?, accuracy: Int) {
        //今回は何もしない
    }

    override fun onSensorChanged(event: SensorEvent?) {
        //センサーの値が変わった時の処理をここに書く
        Log.d("TAG01","センサーが変わりました");
        //引数（イベント）の中身が何もなかったら何もせずに終了
        if(event == null){return;};
        //加速度センサーのイベントか判定
        if(event.sensor.type == Sensor.TYPE_ACCELEROMETER){
            //ログに表示するための文字列を組み立てる
            var str:String = "x = ${event.values[0].toString()}" +
                    ", y = ${event.values[1].toString()}" +
                    ", z = ${event.values[2].toString()}";
            //でバックログに出力
            //Log.d("加速度センサー",str);
        }
        // ボールの描画の計算処理
        if(time==0L){ time = System.currentTimeMillis();} // 最初のタイミングでは現在時刻を保存
        // イベントのセンサー種別の情報がアクセラメーター（加速度センサー）の時だけ以下の処理を実行
        if(event.sensor.type == Sensor.TYPE_ACCELEROMETER){
            // センサーのx(左右),y（縦）値を取得
            val x = event.values[0]*-1;
            val y = event.values[1];

            // 経過時間を計算(今の時間-前の時間 = 経過時間)
            var t = (System.currentTimeMillis() - time).toFloat();
            // 今の時間を「前の時間」として保存
            time = System.currentTimeMillis();
            t /= 1000.0f;

            // 移動距離を計算（ボールをどれくらい動かすか）
            val dx = (vx*t) + (x*t*t)/2.0f; // xの移動距離(メートル)
            val dy = (vy*t) + (y*t*t)/2.0f; // yの移動距離（メートル）
            // メートルをピクセルのcmに補正してボールのX座標に足しこむ=新しいボールのX座標
            ballX += (dx*coef);
            // メートルをピクセルのcmに補正してボールのY座標に足しこむ=新しいボールのY座標
            ballY += (dy*coef);
            // 今の各方向の加速度を更新
            vx +=(x*t);
            vy +=(y*t);

            // 画面の端にきたら跳ね返る処理
            // 左右について
            if( (ballX -radius)<0 && vx<0 ){
                // 左にぶつかった時
                vx = -vx /1.5f;
                ballX = radius;
            }else if( (ballX+radius)>surfaceWidth && vx>0){
                // 右にぶつかった時
                vx = -vx/1.5f;
                ballX = (surfaceWidth-radius);
            }
            // 上下について
            if( (ballY -radius)<0 && vy<0 ){
                // 下にぶつかった時
                vy = -vy /1.5f;
                ballY = radius;
            }else if( (ballY+radius)>surfaceHeight && vy>0 ){
                // 上にぶつかった時
                vy = -vy/1.5f;
                ballY = surfaceHeight -radius;
            }

            // キャンバスに描画
            this.drawCanvas();
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        // サーフェスの幅と高さをプロパティに保存しておく
        surfaceWidth = width;
        surfaceHeight = height;
        // ボールの初期位置を保存しておく
        ballX = width.toFloat();
        ballY = 50f;
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        //加速度センサーの登録を解除する流れ
        // センサーマネージャを取得
        val sensorManager = this.getSystemService(Context.SENSOR_SERVICE) as
                SensorManager;
        // センサーマネージャを通じてOSからリスナー（自分自身）を登録解除
        sensorManager.unregisterListener(this);
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        // 加速度センサーのリスナーを登録する流れ
        // センサーマネージャを取得
        val sensorManager = this.getSystemService(Context.SENSOR_SERVICE)
                as SensorManager;
        // センサーマネージャーから加速度センサーを取得
        val accSensor =
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // 加速度センサーのリスナーをOSに登録
        sensorManager.registerListener(
            this, // リスナー（自クラス）
            accSensor, // 加速度センサー
            SensorManager.SENSOR_DELAY_GAME // センシングの頻度
        )
    }

        // 画面表示・再表示のライフサイクルイベント
    override fun onResume() {
        // 親クラスのonResume()処理
        super.onResume();
        // 自クラスのonResume()処理
        // センサーマネージャをOSから取得
        val sensorManager = this.getSystemService(Context.SENSOR_SERVICE) as SensorManager;
        // 加速度センサー(Accelerometer)を指定してセンサーマネージャからセンサーを取得
        val accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // リスナー登録して加速度センサーの監視を開始
        sensorManager.registerListener(
            this,  // イベントリスナー機能をもつインスタンス（自クラスのインスタンス）
            accSensor, // 監視するセンサー（加速度センサー）
            SensorManager.SENSOR_DELAY_GAME // センサーの更新頻度
        )
    }
    //画面が非表示になるときに呼ばれるコールバックメソッド
    override fun onPause(){
        super.onPause(); // センサーマネージャを取得
        val sensorManager =
            this.getSystemService(Context.SENSOR_SERVICE) as SensorManager;
        // センサーマネージャに登録したリスナーを解除（自分自身を解除）

    }

        private fun drawCanvas(){
            // キャンバスをロックして取得
            val canvas = surfaceView.holder.lockCanvas();
            // キャンバスの背景色を設定
            canvas.drawColor(Color.GREEN);
            // キャンバスに円を描いてボールにする
            canvas.drawCircle(
                ballX, // ボール中心のX座標
                ballY, // ボール中心のY座標
                radius, // 半径
                Paint().apply {
                    color = Color.BLUE; } // ペイントブラシのインスタンス
            );
            canvas.drawCircle(
                450f, // ボール中心のX座標
                300f, // ボール中心のY座標
                radius, // 半径
                Paint().apply {
                    color = Color.BLACK; } // ペイントブラシのインスタンス
            );
            canvas.drawRect(
                600f, // 左上のX座標
                400f, // 左上のY座標
                850f, // 右下のX座標
                450f, //右下のY座標
                Paint().apply {
                    color = Color.BLACK; } // ペイントブラシのインスタンス
            );
            canvas.drawRect(
                200f, // 左上のX座標
                450f, // 左上のY座標
                250f, // 右下のX座標
                550f, //右下のY座標
                Paint().apply {
                    color = Color.BLACK; } // ペイントブラシのインスタンス
            );
            canvas.drawRect(
                650f, // 左上のX座標
                1000f, // 左上のY座標
                900f, // 右下のX座標
                1050f, //右下のY座標
                Paint().apply {
                    color = Color.BLACK; } // ペイントブラシのインスタンス
            );
            canvas.drawRect(
                170f, // 左上のX座標
                1100f, // 左上のY座標
                470f, // 右下のX座標
                1150f, //右下のY座標
                Paint().apply {
                    color = Color.BLACK; } // ペイントブラシのインスタンス
            );


            // キャンバスをアンロック（ロック解除）してキャンバスを描画(ポスト)
            surfaceView.holder.unlockCanvasAndPost(canvas);
        }
}
