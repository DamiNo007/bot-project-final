package kz.coders.chat.gateway.dialogflow

import java.io.FileInputStream
import java.util.UUID
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.{GoogleCredentials, ServiceAccountCredentials}
import com.google.cloud.dialogflow.v2.{DetectIntentRequest, QueryInput, QueryResult, SessionName, SessionsClient, SessionsSettings, TextInput}
import kz.coders.chat.gateway.Boot.config

trait DialogflowConf {

  def getQueryInput(message: String): QueryInput = {
    QueryInput
      .newBuilder()
      .setText(
        TextInput
          .newBuilder()
          .setText(message)
          .setLanguageCode("EN-US")
          .build())
      .build()
  }

  def getDialogflowResponse(message: String): QueryResult = {
    sessionClient.detectIntent(
      DetectIntentRequest
        .newBuilder()
        .setQueryInput(
          getQueryInput(message)
        )
        .setSession(sessionName.toString)
        .build()
    )
      .getQueryResult
  }

  val googleCredentials = GoogleCredentials.fromStream(
    new FileInputStream(config.getString("dialogflow.connection"))
  )

  val projectId = googleCredentials.asInstanceOf[ServiceAccountCredentials].getProjectId

  val sessionClient = SessionsClient
    .create(
      SessionsSettings
        .newBuilder()
        .setCredentialsProvider(FixedCredentialsProvider.create(googleCredentials))
        .build()
    )

  val sessionName = SessionName.of(projectId, UUID.randomUUID().toString)
}
