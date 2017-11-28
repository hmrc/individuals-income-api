/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.individualsincomeapi.cache

import javax.inject.{Inject, Singleton}

import play.api.Configuration
import play.api.libs.json.{Format, JsValue}
import uk.gov.hmrc.cache.TimeToLive
import uk.gov.hmrc.cache.model.Cache
import uk.gov.hmrc.cache.repository.CacheRepository
import uk.gov.hmrc.crypto.json.{JsonDecryptor, JsonEncryptor}
import uk.gov.hmrc.crypto.{AesCrypto, Protected, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ShortLivedCache @Inject()(val configuration: CacheConfiguration, crypto: Crypto) extends TimeToLive {

  lazy val repository = CacheRepository("shortLivedCache", configuration.cacheTtl, Cache.mongoFormats)

  def cache[T](id: String, key: String, value: T)(implicit formats: Format[T]): Future[Unit] = {
    val jsonEncryptor = new JsonEncryptor()(crypto, formats)
    val encryptedValue: JsValue = jsonEncryptor.writes(Protected[T](value))
    repository.createOrUpdate(id, key, encryptedValue).map(_ => ())
  }

  def fetch[T](id: String, key: String)(implicit formats: Format[T]): Future[Option[T]] = {
    val decryptor = new JsonDecryptor()(crypto, formats)

    repository.findById(id) map {
      case Some(cache) => cache.data flatMap { json =>
        (json \ key).toOption flatMap { jsValue =>
          decryptor.reads(jsValue).asOpt map (_.decryptedValue)
        }
      }
      case None => None
    }
  }
}

@Singleton
class CacheConfiguration @Inject()(configuration: Configuration) {
  lazy val enabled = configuration.getBoolean("cache.enabled").getOrElse(true)
  lazy val cacheTtl = configuration.getInt("cache.ttlInSeconds").getOrElse(60 * 15)
}

@Singleton
class Crypto @Inject()(configuration: Configuration) extends CompositeSymmetricCrypto {

  override protected val currentCrypto: Encrypter with Decrypter = new AesCrypto {
    override protected val encryptionKey: String = configuration.getString("cache.encryption.key").getOrElse(throw new RuntimeException("Missing [json.encryption.key] configuration"))
  }

  override protected val previousCryptos = {
    val previousEncryptionKeys = configuration.getStringSeq("cache.encryption.previousKeys").getOrElse(Seq.empty)
    previousEncryptionKeys.map(k => new AesCrypto {
      override val encryptionKey = k
    })
  }
}