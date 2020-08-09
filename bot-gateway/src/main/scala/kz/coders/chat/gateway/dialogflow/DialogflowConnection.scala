package kz.coders.chat.gateway.dialogflow

import java.io.FileInputStream
import java.util.UUID
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.{GoogleCredentials, ServiceAccountCredentials}
import com.google.cloud.dialogflow.v2.{SessionName, SessionsClient, SessionsSettings}
import com.typesafe.config.{Config, ConfigFactory}

object DialogflowConnection {

  val config: Config = ConfigFactory.load()

  private val googleCredentials = GoogleCredentials.fromStream(
    new FileInputStream(config.getString("dialogflow.connection"))
  )

  private val projectId = googleCredentials.asInstanceOf[ServiceAccountCredentials].getProjectId

  val sessionClient: SessionsClient = SessionsClient
    .create(
      SessionsSettings
        .newBuilder()
        .setCredentialsProvider(FixedCredentialsProvider.create(googleCredentials))
        .build()
    )

  val sessionName: SessionName = SessionName.of(projectId, UUID.randomUUID().toString)
}
