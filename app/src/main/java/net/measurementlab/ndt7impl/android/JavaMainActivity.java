package net.measurementlab.ndt7impl.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import net.measurementlab.ndt7.android.models.ClientResponse;
import net.measurementlab.ndt7.android.utils.DataConverter;
import net.measurementlab.ndt7.android.models.Measurement;
import net.measurementlab.ndt7.android.NDTTest;
import net.measurementlab.ndt7.android.models.Settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import okhttp3.OkHttpClient;


public class JavaMainActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Settings settings = new Settings();
        settings.setPort(443);
        settings.setSkipTlsCertificateVerification(true);

        NDTTestImpl ndtTestImpl = new NDTTestImpl(settings, null);

        ndtTestImpl.startTest(NDTTest.TestType.UPLOAD_AND_DOWNLOAD);

        findViewById(R.id.button).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ndtTestImpl.startTest(NDTTest.TestType.UPLOAD_AND_DOWNLOAD);
                    }
                }
        );
    }

    class NDTTestImpl extends NDTTest {

        public NDTTestImpl(@NotNull Settings settings, @Nullable OkHttpClient httpClient) {
            super(settings, httpClient);
        }

        @Override
        public void onDownloadProgress(@NotNull ClientResponse clientResponse) {
            super.onDownloadProgress(clientResponse);
            String speed = DataConverter.convertToMbps(clientResponse);
            System.out.println("Download Progress: " + speed);
            JavaMainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    TextView num = findViewById(R.id.textView);
                    num.setText(speed);
                }
            });
        }

        @Override
        public void onFinished(@Nullable ClientResponse clientResponse, @Nullable Throwable error, @NotNull TestType testType) {
            super.onFinished(clientResponse, error, testType);
            assert clientResponse != null;
            System.out.println("Done Progress: " + DataConverter.convertToMbps(clientResponse));

        }

        @Override
        public void onMeasurementDownloadProgress(@NotNull Measurement measurement) {
            super.onMeasurementDownloadProgress(measurement);
            System.out.println("Measurement download Progress: " + measurement);
        }

        @Override
        public void onMeasurementUploadProgress(@NotNull Measurement measurement) {
            super.onMeasurementUploadProgress(measurement);
            System.out.println("Measurement upload Progress: " + measurement);
        }

        @Override
        public void onUploadProgress(@NotNull ClientResponse clientResponse) {
            super.onUploadProgress(clientResponse);
            String speed = DataConverter.convertToMbps(clientResponse);
            System.out.println("Upload Progress: " + speed);
            JavaMainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    TextView num = findViewById(R.id.textView2);
                    num.setText(speed);
                }
            });
        }
    }
}