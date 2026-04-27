use actix_web::{web, App, HttpServer, HttpResponse, middleware::Logger};
use dashmap::DashMap;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use std::time::{Duration, Instant};
use log::info;

#[derive(Clone)]
struct TokenBucket {
    tokens: f64,
    capacity: f64,
    refill_rate: f64,
    last_refill: Instant,
}

impl TokenBucket {
    fn new(capacity: f64, refill_rate: f64) -> Self {
        Self {
            tokens: capacity,
            capacity,
            refill_rate,
            last_refill: Instant::now(),
        }
    }

    fn refill(&mut self) {
        let now = Instant::now();
        let elapsed = now.duration_since(self.last_refill).as_secs_f64();
        self.tokens = (self.tokens + elapsed * self.refill_rate).min(self.capacity);
        self.last_refill = now;
    }

    fn consume(&mut self, tokens: f64) -> bool {
        self.refill();
        if self.tokens >= tokens {
            self.tokens -= tokens;
            true
        } else {
            false
        }
    }

    fn available(&self) -> f64 {
        self.tokens
    }
}

struct AppState {
    buckets: DashMap<String, TokenBucket>,
}

impl AppState {
    fn new() -> Self {
        Self { buckets: DashMap::new() }
    }
}

#[derive(Deserialize)]
struct CheckRequest {
    key: String,
    #[serde(default = "default_cost")]
    cost: f64,
    #[serde(default = "default_capacity")]
    capacity: f64,
    #[serde(default = "default_refill")]
    refill_per_second: f64,
}

fn default_cost() -> f64 { 1.0 }
fn default_capacity() -> f64 { 60.0 }
fn default_refill() -> f64 { 1.0 }

#[derive(Serialize)]
struct RateLimitResponse {
    allowed: bool,
    key: String,
    tokens_remaining: f64,
    capacity: f64,
}

#[derive(Serialize)]
struct HealthResponse {
    status: &'static str,
    service: &'static str,
    version: &'static str,
    buckets_tracked: usize,
}

async fn health(data: web::Data<Arc<AppState>>) -> HttpResponse {
    HttpResponse::Ok().json(HealthResponse {
        status: "ok",
        service: "dusk-rate-limiter",
        version: "0.1.0",
        buckets_tracked: data.buckets.len(),
    })
}

async fn check_rate(
    data: web::Data<Arc<AppState>>,
    body: web::Json<CheckRequest>,
) -> HttpResponse {
    let mut entry = data.buckets
        .entry(body.key.clone())
        .or_insert_with(|| TokenBucket::new(body.capacity, body.refill_per_second));

    entry.refill();
    let tokens_remaining = entry.available();
    let allowed = tokens_remaining >= body.cost;

    HttpResponse::Ok().json(RateLimitResponse {
        allowed,
        key: body.key.clone(),
        tokens_remaining,
        capacity: body.capacity,
    })
}

async fn consume_token(
    data: web::Data<Arc<AppState>>,
    body: web::Json<CheckRequest>,
) -> HttpResponse {
    let mut entry = data.buckets
        .entry(body.key.clone())
        .or_insert_with(|| TokenBucket::new(body.capacity, body.refill_per_second));

    let allowed = entry.consume(body.cost);
    let tokens_remaining = entry.available();

    if allowed {
        HttpResponse::Ok().json(RateLimitResponse {
            allowed: true,
            key: body.key.clone(),
            tokens_remaining,
            capacity: body.capacity,
        })
    } else {
        HttpResponse::TooManyRequests().json(RateLimitResponse {
            allowed: false,
            key: body.key.clone(),
            tokens_remaining,
            capacity: body.capacity,
        })
    }
}

#[derive(Deserialize)]
struct ResetPath {
    key: String,
}

async fn reset_key(
    data: web::Data<Arc<AppState>>,
    path: web::Path<ResetPath>,
) -> HttpResponse {
    data.buckets.remove(&path.key);
    HttpResponse::Ok().json(serde_json::json!({
        "reset": true,
        "key": path.key
    }))
}

async fn list_buckets(data: web::Data<Arc<AppState>>) -> HttpResponse {
    let keys: Vec<String> = data.buckets.iter().map(|e| e.key().clone()).collect();
    HttpResponse::Ok().json(serde_json::json!({
        "count": keys.len(),
        "keys": keys
    }))
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    dotenv::dotenv().ok();
    env_logger::init_from_env(env_logger::Env::default().default_filter_or("info"));

    let port: u16 = std::env::var("PORT")
        .unwrap_or_else(|_| "8081".to_string())
        .parse()
        .unwrap_or(8081);

    let state = Arc::new(AppState::new());
    info!("Dusk Rate Limiter starting on port {}", port);

    HttpServer::new(move || {
        App::new()
            .wrap(Logger::default())
            .app_data(web::Data::new(state.clone()))
            .route("/health", web::get().to(health))
            .route("/api/check", web::post().to(check_rate))
            .route("/api/consume", web::post().to(consume_token))
            .route("/api/reset/{key}", web::delete().to(reset_key))
            .route("/api/buckets", web::get().to(list_buckets))
    })
    .bind(("0.0.0.0", port))?
    .run()
    .await
}
