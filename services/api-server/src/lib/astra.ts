import cassandra from "cassandra-driver";

const endpoint = process.env["ASTRA_DB_API_ENDPOINT"]!;
const token = process.env["ASTRA_DB_TOKEN"]!;

const hostname = new URL(endpoint).hostname;
const [, , , dbId, region] = hostname.split(".");
const contactPoint = `${dbId}-${region}.db.astra.datastax.com`;

let _client: cassandra.Client | null = null;

export async function getAstraClient(): Promise<cassandra.Client> {
  if (_client) return _client;

  const client = new cassandra.Client({
    contactPoints: [contactPoint],
    localDataCenter: region?.replace(/-\d+$/, "") || "us-east1",
    authProvider: new cassandra.auth.PlainTextAuthProvider("token", token),
    sslOptions: { rejectUnauthorized: true },
    queryOptions: { consistency: cassandra.types.consistencies.localQuorum },
  });

  await client.connect();
  _client = client;
  return client;
}

export async function initAstraSchema(): Promise<void> {
  const client = await getAstraClient();
  const ks = "dusk";

  await client.execute(`
    CREATE KEYSPACE IF NOT EXISTS ${ks}
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}
  `);

  await client.execute(`
    CREATE TABLE IF NOT EXISTS ${ks}.posts_by_user (
      user_id TEXT,
      created_at TIMEUUID,
      post_id TEXT,
      content TEXT,
      media_urls LIST<TEXT>,
      community_id TEXT,
      likes_count COUNTER,
      PRIMARY KEY (user_id, created_at)
    ) WITH CLUSTERING ORDER BY (created_at DESC)
  `);

  await client.execute(`
    CREATE TABLE IF NOT EXISTS ${ks}.feed_by_user (
      user_id TEXT,
      created_at TIMEUUID,
      post_id TEXT,
      author_id TEXT,
      content TEXT,
      PRIMARY KEY (user_id, created_at)
    ) WITH CLUSTERING ORDER BY (created_at DESC)
  `);

  await client.execute(`
    CREATE TABLE IF NOT EXISTS ${ks}.social_graph (
      user_id TEXT,
      edge_type TEXT,
      target_id TEXT,
      created_at TIMESTAMP,
      PRIMARY KEY ((user_id, edge_type), target_id)
    )
  `);

  await client.execute(`
    CREATE TABLE IF NOT EXISTS ${ks}.post_interactions (
      post_id TEXT,
      user_id TEXT,
      action TEXT,
      created_at TIMESTAMP,
      PRIMARY KEY (post_id, user_id, action)
    )
  `);

  await client.execute(`
    CREATE TABLE IF NOT EXISTS ${ks}.user_activity (
      user_id TEXT,
      bucket TEXT,
      event_type TEXT,
      event_data TEXT,
      created_at TIMEUUID,
      PRIMARY KEY ((user_id, bucket), created_at)
    ) WITH CLUSTERING ORDER BY (created_at DESC)
  `);

  await client.execute(`
    CREATE TABLE IF NOT EXISTS ${ks}.trending_posts (
      bucket TEXT,
      score BIGINT,
      post_id TEXT,
      PRIMARY KEY (bucket, score, post_id)
    ) WITH CLUSTERING ORDER BY (score DESC)
  `);
}

export async function recordPostInteraction(
  postId: string,
  userId: string,
  action: "like" | "repost" | "bookmark" | "view"
): Promise<void> {
  const client = await getAstraClient();
  await client.execute(
    `INSERT INTO dusk.post_interactions (post_id, user_id, action, created_at)
     VALUES (?, ?, ?, toTimestamp(now()))`,
    [postId, userId, action],
    { prepare: true }
  );
}

export async function getPostInteractions(
  postId: string,
  action: string,
  limit = 100
): Promise<string[]> {
  const client = await getAstraClient();
  const result = await client.execute(
    `SELECT user_id FROM dusk.post_interactions WHERE post_id = ? AND action = ? LIMIT ?`,
    [postId, action, limit],
    { prepare: true }
  );
  return result.rows.map((r) => r.user_id as string);
}

export async function addToUserFeed(
  userId: string,
  postId: string,
  authorId: string,
  content: string
): Promise<void> {
  const client = await getAstraClient();
  const now = cassandra.types.TimeUuid.now();
  await client.execute(
    `INSERT INTO dusk.feed_by_user (user_id, created_at, post_id, author_id, content)
     VALUES (?, ?, ?, ?, ?)`,
    [userId, now, postId, authorId, content],
    { prepare: true }
  );
}

export async function getUserFeed(
  userId: string,
  limit = 50
): Promise<any[]> {
  const client = await getAstraClient();
  const result = await client.execute(
    `SELECT * FROM dusk.feed_by_user WHERE user_id = ? LIMIT ?`,
    [userId, limit],
    { prepare: true }
  );
  return result.rows;
}

export async function recordActivity(
  userId: string,
  eventType: string,
  eventData: object
): Promise<void> {
  const client = await getAstraClient();
  const bucket = new Date().toISOString().slice(0, 7);
  const now = cassandra.types.TimeUuid.now();
  await client.execute(
    `INSERT INTO dusk.user_activity (user_id, bucket, event_type, event_data, created_at)
     VALUES (?, ?, ?, ?, ?)`,
    [userId, bucket, eventType, JSON.stringify(eventData), now],
    { prepare: true }
  );
}
