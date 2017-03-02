import com.google.common.collect.Table;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import util.BenchmarkConfig;
import util.Util;

import java.io.IOException;
import java.util.Random;

public class ReadCompute extends Configured implements Tool {

  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new Configuration(), new ReadCompute(), args);
    System.exit(res);
  }

  @Override public int run(String[] args) throws Exception {
    Util.checkArgs(args, 2,
        "ReadCompute.jar input computeTime(s) [optional arguments]");
    Configuration conf = new Configuration();
    Path inPath = new Path(args[0]);
    long computeTime = Long.parseLong(args[1]);
    Path outPath = new Path(conf.get(BenchmarkConfig.BENCHMARK_TMPPATH,
        BenchmarkConfig.BENCHMARK_TMPPATH_DEFAULT) + "ReadCompute",
        inPath.getName() + "_read_out");
    conf.setLong("perforator.map.computetime", computeTime * 1000);
    Util.setConfParameters(conf, args, 2);

    Job job = Job.getInstance(conf, "ReadCompute " + inPath);
    job.setJarByClass(ReadCompute.class);
    job.setMapperClass(MapClass.class);
    job.setNumReduceTasks(0);

    FileInputFormat.addInputPaths(job, args[0]);
    FileOutputFormat.setOutputPath(job, outPath);
    boolean isSuccessful = job.waitForCompletion(true);
    String historyURI = Util.getHistoryURI(conf);

    Table<String, String, Double> times = Util
        .getTaskStats(job.getJobID().toString(), historyURI,
            Util.TASK_TYPE.MAP);
    System.out.println(job.getJobID());
    System.out.println("Mean : " + Util.getMean(times.values()) / 1000);
    System.out.println("Variance :" + Util.getVariance(times.values()) / 1000000);
    System.out.println("Num of Maps :" + times.values().size());

    Util.cleanup(conf,inPath, outPath);
    return isSuccessful ? 0 : 1;
  }

  public static class MapClass
      extends Mapper<LongWritable, Text, Text, IntWritable> {
    public void map(LongWritable key, Text value, Context context)
        throws IOException, InterruptedException {
      long startTime = System.currentTimeMillis();
      long endTime = startTime + context.getConfiguration()
          .getLong("perforator.map.computetime", 10);
      int count = 0;
      while (true) {
        count += new Random().nextInt();
        if (System.currentTimeMillis() >= endTime) {
          break;
        }
      }
    }
  }
}
