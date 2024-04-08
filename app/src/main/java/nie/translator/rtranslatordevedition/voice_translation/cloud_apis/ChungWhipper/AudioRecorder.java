package nie.translator.rtranslatordevedition.voice_translation.cloud_apis.ChungWhipper;
import android.media.MediaRecorder;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
public class AudioRecorder {

    private static final String OUTPUT_FILE = "CHUNGrecorded_audio.mp3";

    private MediaRecorder mediaRecorder;
    private File outputFile;

    public void startRecording() {
        try {
            stopRecording(); // Stop previous recording if any

            mediaRecorder = new MediaRecorder();



            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            //mediaRecorder.setAudioChannels(1);
            //mediaRecorder.setAudioSamplingRate(44100);



            File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/YourAppName");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            outputFile = new File(directory, OUTPUT_FILE);

            mediaRecorder.setOutputFile(outputFile.getAbsolutePath());

            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    public File getRecordedAudioFile() {
        return outputFile;
    }
}