package hadoop.task4

import java.io.File

import ch.qos.logback.classic.util.ContextInitializer
import hadoop.Constants.{hdfsOutputPath, localInputPathName, numInputs}
import hadoop.task4.Task4Constants.localOutputPathName
import org.apache.hadoop.conf.Configured
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.{FileInputFormat, KeyValueTextInputFormat}
import org.apache.hadoop.mapreduce.lib.output.{FileOutputFormat, TextOutputFormat}
import org.apache.hadoop.util.{Tool, ToolRunner}
import org.slf4j.{Logger, LoggerFactory}

object Driver extends Configured with Tool {
  System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "src/main/resources/configuration/logback.xml")
  val logger: Logger = LoggerFactory.getLogger(Driver.getClass)

  def main(args: Array[String]): Unit = {
    val exitCode = ToolRunner.run(Driver, args)
    System.exit(exitCode)
  }

  @throws[Exception]
  def run(args: Array[String]): Int = {

    val job: Job = new Job()
    job.setJarByClass(Driver.getClass)
    job.setJobName("MaximumAuthorCountComputer")

    val fs: FileSystem = FileSystem.get(job.getConfiguration)

    val localInputPathNames = getConcatenatedLocalInputPathNames(0)
    val hdfsInputPathNames = getConcatenatedHdfsInputPathNames(0)

    val localOutputPath = new Path(localOutputPathName)
    val localOutputDir = new File(localOutputPathName)

    job.setInputFormatClass(classOf[KeyValueTextInputFormat])
    job.setOutputKeyClass(classOf[Text])
    job.setOutputValueClass(classOf[Text])
    job.setOutputFormatClass(classOf[TextOutputFormat[Text, Text]])

    FileInputFormat.addInputPaths(job, hdfsInputPathNames.mkString(","))
    for (i <- 0 until numInputs) fs.copyFromLocalFile(new Path(localInputPathNames(i)), new Path(hdfsInputPathNames(i)))
    FileOutputFormat.setOutputPath(job, hdfsOutputPath)

    job.setMapperClass(classOf[MyMapper])
    job.setReducerClass(classOf[MyReducer])

    if (fs.exists(hdfsOutputPath)) fs.delete(hdfsOutputPath, true)

    val returnValue = if (job.waitForCompletion(true)) 0 else 1

    if (job.isSuccessful) {
      println("Job successful")
      logger.info("JOB SUCCESSFUL")
    }
    else {
      println("Job not successful")
      logger.info("JOB FAILED")
    }

    if (!localOutputDir.exists()) localOutputDir.mkdir()
    fs.copyToLocalFile(hdfsOutputPath, localOutputPath)
    fs.delete(hdfsOutputPath, true)
    returnValue
  }

  /**
   * This function will put all 138 hdfs input path dir names into a list and return it
   */
  def getConcatenatedHdfsInputPathNames(index: Int): List[String] =
    if (index == numInputs-1) List(s"input$index")
    else List(s"input$index") ::: getConcatenatedHdfsInputPathNames(index+1)

  /**
   * This function will put all 138 local input path dir relative names into a list and return it
   */
  def getConcatenatedLocalInputPathNames(index: Int): List[String] = {
    if (index == numInputs-1) List(s"$localInputPathName/input$index.txt")
    else List(s"$localInputPathName/input$index.txt") ::: getConcatenatedLocalInputPathNames(index+1)
  }
}
