/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.source.mysql;

import io.airbyte.cdk.testutils.ContainerFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

public class MySQLContainerFactory extends ContainerFactory<MySQLContainer<?>> {
  final static Logger logger = LoggerFactory.getLogger(MySQLContainerFactory.class);

  @Override
  protected MySQLContainer<?> createNewContainer(DockerImageName imageName) {
    var container = new MySQLContainer<>(imageName.asCompatibleSubstituteFor("mysql"))
        .withLogConsumer(new Slf4jLogConsumer(logger));

    execInContainer(container, "sed -i '31 a bind_address=0.0.0.0' /etc/my.cnf");
    return container;
  }

  /**
   * Create a new network and bind it to the container.
   */
  public void withNetwork(MySQLContainer<?> container) {
    container.withNetwork(Network.newNetwork());
  }

  private static final String INVALID_TIMEZONE_CEST = "CEST";

  public void withInvalidTimezoneCEST(MySQLContainer<?> container) {
    container.withEnv("TZ", INVALID_TIMEZONE_CEST);
  }

  public void withMoscowTimezone(MySQLContainer<?> container) {
    container.withEnv("TZ", "Europe/Moscow");
  }

  public void withRootAndServerCertificates(MySQLContainer<?> container) {
    execInContainer(container,
        "sed -i '31 a ssl' /etc/my.cnf",
        "sed -i '32 a ssl-ca=/var/lib/mysql/ca.pem' /etc/my.cnf",
        "sed -i '33 a ssl-cert=/var/lib/mysql/server-cert.pem' /etc/my.cnf",
        "sed -i '34 a ssl-key=/var/lib/mysql/server-key.pem' /etc/my.cnf",
        "sed -i '35 a require_secure_transport=ON' /etc/my.cnf");
  }

  public void withClientCertificate(MySQLContainer<?> container) {
    execInContainer(container,
        "sed -i '39 a [client]' /etc/mysql/my.cnf",
        "sed -i '40 a ssl-ca=/var/lib/mysql/ca.pem' /etc/my.cnf",
        "sed -i '41 a ssl-cert=/var/lib/mysql/client-cert.pem' /etc/my.cnf",
        "sed -i '42 a ssl-key=/var/lib/mysql/client-key.pem' /etc/my.cnf");
  }

  static private void execInContainer(MySQLContainer<?> container, String... commands) {
    container.start();
    try {
      for (String command : commands) {
        var result = container.execInContainer("sh", "-c", command);
        logger.atInfo().log("exec result: " + result.getStdout());
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
