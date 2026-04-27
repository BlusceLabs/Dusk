package com.dusk.feed

import cask.*
import upickle.default.*

case class Post(
  id: String,
  authorId: String,
  content: String,
  likes: Int,
  comments: Int,
  reposts: Int,
  createdAt: Long,
  hashtags: List[String] = List.empty
) derives ReadWriter

case class FeedItem(
  post: Post,
  score: Double,
  reason: String
) derives ReadWriter

case class FanoutRequest(
  postId: String,
  authorId: String,
  followerIds: List[String]
) derives ReadWriter

case class RankRequest(
  posts: List[Post],
  userId: String,
  interests: List[String] = List.empty
) derives ReadWriter

object FeedEngine {
  def score(post: Post, interests: List[String]): Double = {
    val recencyScore = {
      val ageMs = System.currentTimeMillis() - post.createdAt
      val ageHours = ageMs / 3_600_000.0
      math.exp(-ageHours / 24.0) * 3.0
    }
    val engagementScore = {
      val likeScore  = math.log1p(post.likes)  * 0.8
      val cmtScore   = math.log1p(post.comments) * 1.2
      val rpScore    = math.log1p(post.reposts)  * 1.0
      likeScore + cmtScore + rpScore
    }
    val interestScore = {
      val content = post.content.toLowerCase
      val tags    = post.hashtags.map(_.toLowerCase)
      interests.count(i => content.contains(i) || tags.contains(i)) * 1.5
    }
    recencyScore + engagementScore + interestScore + scala.util.Random.nextDouble() * 0.1
  }

  def rank(posts: List[Post], userId: String, interests: List[String]): List[FeedItem] =
    posts
      .map(p => FeedItem(p, score(p, interests), reasonFor(p, interests)))
      .sortBy(-_.score)

  private def reasonFor(post: Post, interests: List[String]): String = {
    val tags = post.hashtags.map(_.toLowerCase)
    val hit  = interests.find(i => tags.contains(i) || post.content.toLowerCase.contains(i))
    hit.map(i => s"Trending in #$i").getOrElse(
      if post.likes > 500 then "Popular post" else "In your feed"
    )
  }
}

object Main extends MainRoutes {
  override def host: String = "0.0.0.0"
  override def port: Int =
    sys.env.get("PORT").flatMap(_.toIntOption).getOrElse(8082)

  @get("/health")
  def health() = ujson.Obj(
    "status"  -> "ok",
    "service" -> "dusk-feed-engine",
    "version" -> "0.1.0",
    "lang"    -> "Scala 3"
  )

  @post("/api/feed/rank")
  def rankFeed(request: Request) = {
    val body    = read[RankRequest](request.text())
    val ranked  = FeedEngine.rank(body.posts, body.userId, body.interests)
    write(ranked)
  }

  @post("/api/feed/fanout")
  def fanout(request: Request) = {
    val body = read[FanoutRequest](request.text())
    ujson.Obj(
      "postId"       -> body.postId,
      "authorId"     -> body.authorId,
      "deliveredTo"  -> body.followerIds.length,
      "followerIds"  -> write(body.followerIds),
      "status"       -> "queued"
    )
  }

  @get("/api/feed/trending")
  def trending() = {
    val tags = List("DuskVibes", "GoldenHour", "NeonAesthetic", "LoFiBeats", "CreatorEconomy")
    write(tags.zipWithIndex.map { case (tag, i) =>
      ujson.Obj("tag" -> tag, "rank" -> (i + 1), "velocity" -> scala.util.Random.nextInt(500))
    })
  }

  initialize()
}
