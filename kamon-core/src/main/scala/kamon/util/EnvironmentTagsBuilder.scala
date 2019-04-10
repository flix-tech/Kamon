package kamon
package util

import com.typesafe.config.Config
import kamon.status.Environment
import kamon.tag.TagSet

import scala.collection.JavaConverters._


/**
  * Utility class for creating TagSet instances out of Environment instances. When an Environment is turned into tags
  * it will generate the following pairs:
  *
  *   - "service", with the service name from the Environment
  *   - "host", with the host name from the Environment
  *   - "instance", with the instance name from the Environment
  *   - One additional pair for each Environment tag, unless exclusions are provided when transforming.
  *
  * Most uses of this class are expected to happen in reporter modules, where the Environment information should usually
  * be exposed along with the metrics and spans.
  */
object EnvironmentTagsBuilder {

  /**
    * Returns a TagSet instance with information from the provided Environment, using the provided config path to
    * retrieve the settings for the transformation from Kamon's Config. The configuration on the provided path is
    * expected to have the following structure:
    *
    * config {
    *   include-host = yes
    *   include-service = yes
    *   include-instance = yes
    *   exclude = [ ]
    * }
    *
    * If any of the settings are missing this function will default to include all Environment information.
    */
  def toTags(environment: Environment, path: String): TagSet =
    toTags(environment, Kamon.config().getConfig(path))

  /**
    * Returns a TagSet instance with information from the provided Environment, using the provided Config to read the
    * configuration settings for the transformation. The configuration is expected to have the following structure:
    *
    * config {
    *   include-host = yes
    *   include-service = yes
    *   include-instance = yes
    *   exclude = [ ]
    * }
    *
    * If any of the settings are missing this function will default to include all Environment information.
    */
  def toTags(environment: Environment, config: Config): TagSet = {
    val includeHost = if(config.hasPath("include-host")) config.getBoolean("include-host") else true
    val includeService = if(config.hasPath("include-service")) config.getBoolean("include-service") else true
    val includeInstance = if(config.hasPath("include-instance")) config.getBoolean("include-instance") else true
    val exclude = if(config.hasPath("exclude")) config.getStringList("exclude").asScala.toSet else Set.empty[String]

    toTags(environment, includeService, includeHost, includeInstance, exclude)
  }

  /**
    * Turns the information enclosed in the provided Environment instance into a TagSet.
    */
  def toTags(environment: Environment, includeService: Boolean, includeHost: Boolean, includeInstance: Boolean,
      exclude: Set[String]): TagSet = {

    val tagSet = TagSet.builder()

    if(includeService)
      tagSet.add(TagKeys.Service, environment.service)

    if(includeHost)
      tagSet.add(TagKeys.Host, environment.host)

    if(includeInstance)
      tagSet.add(TagKeys.Instance, environment.instance)

    // We know for sure that all environment tags are treated as Strings
    environment.tags.iterator(_.toString).foreach { pair =>
      if(!exclude.contains(pair.key)) tagSet.add(pair.key, pair.value)
    }

    tagSet.create()
  }

  object TagKeys {
    val Host = "host"
    val Service = "service"
    val Instance = "instance"
  }
}
