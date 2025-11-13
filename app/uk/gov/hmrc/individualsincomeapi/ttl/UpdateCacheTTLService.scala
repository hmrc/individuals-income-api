/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.individualsincomeapi.ttl

import javax.inject.{Inject, Singleton}
import com.mongodb.ErrorCategory
import org.bson.BsonType
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Document, MongoCollection, MongoWriteException, SingleObservableFuture}
import play.api.Logging
import uk.gov.hmrc.individualsincomeapi.cache.v1
import uk.gov.hmrc.individualsincomeapi.cache.v2.CacheRepositoryConfiguration
import uk.gov.hmrc.mongo.MongoComponent

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateCacheTTLService @Inject() (
  mongo: MongoComponent,
  val cacheConfigV2: CacheRepositoryConfiguration,
  val cacheConfigV1: v1.CacheRepositoryConfiguration
)(implicit val ec: ExecutionContext)
    extends Logging {

  private val collectionV2: MongoCollection[Document] =
    mongo.database.getCollection(cacheConfigV2.collName)
  private val collectionV1: MongoCollection[Document] =
    mongo.database.getCollection(cacheConfigV1.collName)
  private val lockCollection: MongoCollection[Document] =
    mongo.database.getCollection("individuals-income-cache-lock")

  private val lockId = "update-cache-ttl-lock"

  // Trigger at the time of Startup
  for {
    _ <- updateItem(collectionV1)
    _ <- updateItem(collectionV2)
  } yield ()

  private def acquireLock(): Future[Boolean] = {
    val lockDoc = Document("_id" -> lockId, "createdAt" -> new Date())
    lockCollection.insertOne(lockDoc).toFuture().map(_ => true).recover {
      case ex: MongoWriteException if ex.getError.getCategory == ErrorCategory.DUPLICATE_KEY =>
        logger.info("Lock already exists. Skipping update job.")
        false
      case ex =>
        logger.error("Unexpected error while acquiring lock", ex)
        false
    }
  }

  private def dropLockCollection(reason: String): Future[Unit] = {
    logger.warn(s"Dropping lock collection due to: $reason")
    lockCollection
      .drop()
      .toFuture()
      .map { _ =>
        logger.info("Lock collection dropped successfully.")
      }
      .recover { case ex =>
        logger.error("Failed to drop lock collection", ex)
      }
  }

  private def updateItem(collection: MongoCollection[Document]): Future[Unit] =
    acquireLock().flatMap {
      case true =>
        logger.info("Lock acquired. Starting aggregation-based update.")
        val lastUpdatedFilter = Filters.`type`("modifiedDetails.lastUpdated", BsonType.STRING)
        val createdATFilter = Filters.`type`("modifiedDetails.createdAt", BsonType.STRING)

        val updatePipeline = List(
          Document(
            "$set" -> Document(
              "modifiedDetails.lastUpdated" -> Document("$toDate" -> "$modifiedDetails.lastUpdated"),
              "modifiedDetails.createdAt"   -> Document("$toDate" -> "$modifiedDetails.createdAt")
            )
          )
        )

        collection
          .updateMany(
            Filters.and(lastUpdatedFilter, createdATFilter),
            updatePipeline
          )
          .toFuture()
          .map { result =>
            logger.info(s"Aggregation update completed: ${result.getModifiedCount} documents updated.")
          }
          .recoverWith { case ex =>
            logger.error("Aggregation update failed", ex)
            // Drop collection in case of failure
            dropLockCollection("Aggregation failure")
          }
          .flatMap { _ =>
            // Drop collection after successful update
            dropLockCollection("Successful update")
          }

      case false =>
        Future.successful(())
    }
}
