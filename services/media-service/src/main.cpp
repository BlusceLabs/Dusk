#include <iostream>
#include <string>
#include <map>
#include <vector>
#include <mutex>
#include <thread>
#include <chrono>
#include <random>
#include <sstream>
#include <cstdlib>

#define CPPHTTPLIB_OPENSSL_SUPPORT 0
#include "httplib.h"

using namespace std;
using json_map = map<string, string>;

// ── Job registry ──────────────────────────────────────────────────────────────
enum class JobStatus { QUEUED, PROCESSING, DONE, FAILED };

struct TranscodeJob {
    string id;
    string inputUrl;
    string outputFormat;
    int    targetWidth;
    int    targetHeight;
    int    bitrate;
    JobStatus status;
    string createdAt;
    string completedAt;
    string outputUrl;
    string error;
};

mutex            jobs_mutex;
map<string, TranscodeJob> jobs;

string generate_id() {
    static mt19937 rng(chrono::steady_clock::now().time_since_epoch().count());
    uniform_int_distribution<int> dist(0, 15);
    const char* hex = "0123456789abcdef";
    string id = "job_";
    for (int i = 0; i < 12; ++i) id += hex[dist(rng)];
    return id;
}

string now_iso() {
    auto t = chrono::system_clock::now();
    auto tt = chrono::system_clock::to_time_t(t);
    char buf[32];
    strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%SZ", gmtime(&tt));
    return string(buf);
}

string status_str(JobStatus s) {
    switch (s) {
        case JobStatus::QUEUED:     return "queued";
        case JobStatus::PROCESSING: return "processing";
        case JobStatus::DONE:       return "done";
        case JobStatus::FAILED:     return "failed";
    }
    return "unknown";
}

string job_to_json(const TranscodeJob& j) {
    ostringstream oss;
    oss << "{"
        << "\"id\":\"" << j.id << "\","
        << "\"status\":\"" << status_str(j.status) << "\","
        << "\"inputUrl\":\"" << j.inputUrl << "\","
        << "\"outputFormat\":\"" << j.outputFormat << "\","
        << "\"targetWidth\":" << j.targetWidth << ","
        << "\"targetHeight\":" << j.targetHeight << ","
        << "\"bitrate\":" << j.bitrate << ","
        << "\"createdAt\":\"" << j.createdAt << "\","
        << "\"completedAt\":\"" << j.completedAt << "\","
        << "\"outputUrl\":\"" << j.outputUrl << "\","
        << "\"error\":\"" << j.error << "\""
        << "}";
    return oss.str();
}

// ── Background worker ─────────────────────────────────────────────────────────
void process_job(const string& job_id) {
    this_thread::sleep_for(chrono::seconds(2));

    lock_guard<mutex> lock(jobs_mutex);
    auto it = jobs.find(job_id);
    if (it == jobs.end()) return;

    auto& job = it->second;
    job.status = JobStatus::PROCESSING;

    this_thread::sleep_for(chrono::seconds(3));

    job.status      = JobStatus::DONE;
    job.completedAt = now_iso();
    job.outputUrl   = "https://cdn.dusk.app/media/transcoded/" + job_id + "." + job.outputFormat;

    cout << "[media] Job " << job_id << " completed" << endl;
}

// ── Simple JSON parser helper ─────────────────────────────────────────────────
string json_string(const string& body, const string& key, const string& def = "") {
    string search = "\"" + key + "\":\"";
    size_t pos = body.find(search);
    if (pos == string::npos) return def;
    pos += search.size();
    size_t end = body.find('"', pos);
    if (end == string::npos) return def;
    return body.substr(pos, end - pos);
}

int json_int(const string& body, const string& key, int def = 0) {
    string search = "\"" + key + "\":";
    size_t pos = body.find(search);
    if (pos == string::npos) return def;
    pos += search.size();
    try { return stoi(body.substr(pos)); } catch (...) { return def; }
}

// ── Main ──────────────────────────────────────────────────────────────────────
int main() {
    const char* port_env = getenv("PORT");
    int port = port_env ? atoi(port_env) : 8084;

    httplib::Server svr;

    svr.Get("/health", [](const httplib::Request&, httplib::Response& res) {
        lock_guard<mutex> lock(jobs_mutex);
        ostringstream body;
        body << "{"
             << "\"status\":\"ok\","
             << "\"service\":\"dusk-media-pipeline\","
             << "\"version\":\"0.1.0\","
             << "\"lang\":\"C++17 (GCC)\","
             << "\"jobs_queued\":" << jobs.size()
             << "}";
        res.set_content(body.str(), "application/json");
    });

    svr.Post("/api/transcode", [](const httplib::Request& req, httplib::Response& res) {
        auto input_url  = json_string(req.body, "inputUrl");
        auto format     = json_string(req.body, "outputFormat", "mp4");
        auto width      = json_int(req.body, "width", 1280);
        auto height     = json_int(req.body, "height", 720);
        auto bitrate    = json_int(req.body, "bitrate", 2000);

        if (input_url.empty()) {
            res.status = 400;
            res.set_content("{\"error\":\"inputUrl is required\"}", "application/json");
            return;
        }

        TranscodeJob job;
        job.id           = generate_id();
        job.inputUrl     = input_url;
        job.outputFormat = format;
        job.targetWidth  = width;
        job.targetHeight = height;
        job.bitrate      = bitrate;
        job.status       = JobStatus::QUEUED;
        job.createdAt    = now_iso();

        {
            lock_guard<mutex> lock(jobs_mutex);
            jobs[job.id] = job;
        }

        thread worker(process_job, job.id);
        worker.detach();

        cout << "[media] Queued job " << job.id << " for " << input_url << endl;
        res.status = 202;
        res.set_content(job_to_json(job), "application/json");
    });

    svr.Get(R"(/api/job/([a-z0-9_]+))", [](const httplib::Request& req, httplib::Response& res) {
        auto job_id = req.matches[1].str();
        lock_guard<mutex> lock(jobs_mutex);
        auto it = jobs.find(job_id);
        if (it == jobs.end()) {
            res.status = 404;
            res.set_content("{\"error\":\"job not found\"}", "application/json");
            return;
        }
        res.set_content(job_to_json(it->second), "application/json");
    });

    svr.Get("/api/jobs", [](const httplib::Request&, httplib::Response& res) {
        lock_guard<mutex> lock(jobs_mutex);
        ostringstream oss;
        oss << "[";
        bool first = true;
        for (auto& [id, j] : jobs) {
            if (!first) oss << ",";
            oss << job_to_json(j);
            first = false;
        }
        oss << "]";
        res.set_content(oss.str(), "application/json");
    });

    cout << "[Dusk Media Pipeline] Listening on port " << port << endl;
    svr.listen("0.0.0.0", port);
    return 0;
}
