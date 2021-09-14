import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegProgress;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResult;
import com.github.kokorin.jaffree.ffmpeg.NullOutput;
import com.github.kokorin.jaffree.ffmpeg.ProgressListener;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;

/*
 * Created by Eldevan Nery Junior on 14/09/2021 11:13
 */
public class Main {
    public static void main(String[] args) {
        final AtomicLong durationMillis = new AtomicLong();

        FFmpegResult ffmpegResult = FFmpeg.atPath()
                .addInput(
                        UrlInput.fromUrl("/tmp/reuniao.mp4")
                )
                .addOutput(new NullOutput())
                .setProgressListener(new ProgressListener() {
                    @Override
                    public void onProgress(FFmpegProgress progress) {
                        durationMillis.set(progress.getTime(TimeUnit.MINUTES));
                    }
                })
                .execute();

        System.out.println("Exact duration: " + durationMillis.get() + " Min");
    }
}
