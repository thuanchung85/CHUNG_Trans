package nie.translator.rtranslatordevedition.voice_translation.cloud_apis.myWhipper;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenAIWhisperSTT extends AsyncTask<File, Void, String> {
    static File copyF = new File( new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/YourAppName"),
            "CHUNGrecorded_audio_tempCopy.wav");
    private static final String OPENAI_API_KEY = "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";
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/audio/transcriptions";

    public OpenAIWhisperSTT(AsyncTaskListener listener) {
        this.listener = listener;
    }


    public static String transcribeAudio() throws IOException {
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/YourAppName");
        if (directory.exists()) {
            String OUTPUT_FILE = "CHUNGrecorded_audio.wav";
            File audioFile = new File(directory, OUTPUT_FILE);
            System.out.println(audioFile);
            if (audioFile.exists()) {

              return  copy(audioFile, copyF);

            }
        }
        return "error in wav file";
    }

    public static String copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();

                OkHttpClient client = new OkHttpClient();

                String currentlang = "en";
                currentlang = Locale.getDefault().getLanguage();
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("model", "whisper-1")
                        .addFormDataPart("response_format", "text")
                        .addFormDataPart("language", currentlang)
                        .addFormDataPart("file",copyF.getPath(),
                                RequestBody.create(MediaType.parse("audio/wav"), copyF))
                        .build();

                Request request = new Request.Builder()

                        .url(OPENAI_API_URL)
                        .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                        .addHeader("Content-Type", "multipart/form-data")
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }
                    String responseBody = response.body().string();
                    return responseBody; // Response body contains the transcription
                }
            }
        } finally {
            in.close();
        }
    }

    private Exception exception;

    @Override
    protected String doInBackground(File... files) {
        try {
            String txt = transcribeAudio();
            System.out.println(txt);
            return txt;
        }catch (IOException error){
            Log.d("CHUNG-", "CHUNG- WHIPPER() ERROR -> IOException() " + error.getMessage());
        }

        return null;
    }
    private AsyncTaskListener listener;
    @Override
    protected void onPostExecute(String result) {
        if (exception != null) {
            // Handle exception
            exception.printStackTrace();
        } else {
            // Process result

            System.out.println("Transcription: " + result);
            if (listener != null) {
                listener.onTaskComplete(result);
            }
        }
    }
}

