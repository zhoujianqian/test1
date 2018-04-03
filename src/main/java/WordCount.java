/**
 * MIT.
 * Author: wangxiaolei(鐜嬪皬闆�).
 * Date:17-2-20.
 * Project:ApacheBeamWordCount.
 */


import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.transforms.Aggregator;
import org.apache.beam.sdk.transforms.Count;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.transforms.Sum;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;


public class WordCount {

	private static class PrintFn<T> extends DoFn<T, T> {
		@ProcessElement
		public void processElement(ProcessContext c) throws Exception {
			System.out.println(c.element().toString());
		}
	}
    /**
     *1.a.閫氳繃Dofn缂栫▼Pipeline浣垮緱浠ｇ爜寰堢畝娲併�俠.瀵硅緭鍏ョ殑鏂囨湰鍋氬崟璇嶅垝鍒嗭紝杈撳嚭銆�
     */
    static class ExtractWordsFn extends DoFn<String, String> {
        private final Aggregator<Long, Long> emptyLines =
                createAggregator("emptyLines", Sum.ofLongs());

        @ProcessElement
        public void ll(ProcessContext c) {
            if (c.element().trim().isEmpty()) {
                emptyLines.addValue(1L);
            }

            // 灏嗘枃鏈鍒掑垎涓哄崟璇�
            String[] words = c.element().split("[^a-zA-Z']+");
            // 杈撳嚭PCollection涓殑鍗曡瘝
            for (String word : words) {
                if (!word.isEmpty()) {
                    c.output(word);
                }
            }
        }
    }

    /**
     *2.鏍煎紡鍖栬緭鍏ョ殑鏂囨湰鏁版嵁锛屽皢杞崲鍗曡瘝涓哄苟璁℃暟鐨勬墦鍗板瓧绗︿覆銆�
     */
    public static class FormatAsTextFn extends SimpleFunction<KV<String, Long>, String> {
        @Override
        public String apply(KV<String, Long> input) {
            return input.getKey() + ": " + input.getValue();
        }
    }
    /**
     *3.鍗曡瘝璁℃暟锛孭Transform(PCollection Transform)灏哖Collection鐨勬枃鏈杞崲鎴愭牸寮忓寲鐨勫彲璁℃暟鍗曡瘝銆�
     */
    public static class CountWords extends PTransform<PCollection<String>,
            PCollection<KV<String, Long>>> {
        @Override
        public PCollection<KV<String, Long>> expand(PCollection<String> lines) {

            // 灏嗘枃鏈杞崲鎴愬崟涓崟璇�
            PCollection<String> words = lines.apply(
                    ParDo.of(new ExtractWordsFn()));

            // 璁＄畻姣忎釜鍗曡瘝娆℃暟
            PCollection<KV<String, Long>> wordCounts =
                    words.apply(Count.<String>perElement());

            return wordCounts;
        }
    }

    /**
     *4.鍙互鑷畾涔変竴浜涢�夐」锛圤ptions锛夛紝姣斿鏂囦欢杈撳叆杈撳嚭璺緞
     */
    public interface WordCountOptions extends PipelineOptions {

        /**
         * 鏂囦欢杈撳叆閫夐」锛屽彲浠ラ�氳繃鍛戒护琛屼紶鍏ヨ矾寰勫弬鏁帮紝璺緞榛樿涓篻s://apache-beam-samples/shakespeare/kinglear.txt
         */
        @Description("Path of the file to read from")
        @Default.String("hdfs://172.17.168.96:9000/user/hadoop/beamtest1/test2.txt")
        String getInputFile();
        void setInputFile(String value);

        /**
         * 璁剧疆缁撴灉鏂囦欢杈撳嚭璺緞,鍦╥ntellij IDEA鐨勮繍琛岃缃�夐」涓垨鑰呭湪鍛戒护琛屼腑鎸囧畾杈撳嚭鏂囦欢璺緞锛屽./pom.xml
         */
        @Description("Path of the file to write to")
        @Required
        @Default.String("hdfs://172.17.168.96:9000/user/hadoop/beamtest1/test3.txt")
        String getOutput();
        void setOutput(String value);
    }
    /**
     * 5.杩愯绋嬪簭
     */
    public static void main(String[] args) {
        WordCountOptions options = PipelineOptionsFactory.fromArgs(args).withValidation()
                .as(WordCountOptions.class);
        Pipeline p = Pipeline.create(options);

        p.apply("ReadLines", TextIO.Read.from(options.getInputFile()))
                .apply(new CountWords())
                .apply(MapElements.via(new FormatAsTextFn()))
                .apply(ParDo.of(new PrintFn<>()));
                //.apply("WriteCounts", TextIO.Write.to(options.getOutput()));

        p.run().waitUntilFinish();
    }
}