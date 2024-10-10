#include <iostream>
#include <chrono>
#include <thread>
#include <mutex>
#include <atomic>
#include <functional>
#include <stdexcept>
#include <map>

// Enum representing the states of the circuit breaker
enum class CircuitState {
    CLOSED,   // Normal state, all requests go through
    OPEN,     // Circuit is open, block requests
    HALF_OPEN // Circuit is half-open, allow limited requests to test service recovery
};

class CircuitBreaker {
public:
    CircuitBreaker(int failureThreshold, int retryTime, int successThreshold)
        : failureThreshold_(failureThreshold),
          retryTime_(retryTime),
          successThreshold_(successThreshold),
          failureCount_(0),
          successCount_(0),
          state_(CircuitState::CLOSED),
          lastFailureTime_(std::chrono::system_clock::now()) {}

    // Method to handle calls to external service
    template<typename Func>
    auto execute(Func func) {
        std::lock_guard<std::mutex> lock(mutex_);

        if (state_ == CircuitState::OPEN) {
            // If circuit is open, check if we should attempt a retry
            if (std::chrono::duration_cast<std::chrono::seconds>(
                std::chrono::system_clock::now() - lastFailureTime_).count() >= retryTime_) {
                state_ = CircuitState::HALF_OPEN;
            } else {
                return fallback();  // Circuit open, return fallback
            }
        }

        try {
            auto result = func();  // Attempt the call
            onSuccess();
            return result;
        } catch (const std::exception& e) {
            onFailure();
            return fallback();  // On failure, provide fallback
        }
    }

    // Method to expose circuit breaker metrics
    std::map<std::string, int> getMetrics() {
        return {
            {"failureCount", failureCount_},
            {"successCount", successCount_},
            {"currentState", static_cast<int>(state_)}
        };
    }

private:
    std::mutex mutex_;
    int failureThreshold_;
    int retryTime_;
    int successThreshold_;
    std::atomic<int> failureCount_;
    std::atomic<int> successCount_;
    CircuitState state_;
    std::chrono::system_clock::time_point lastFailureTime_;

    // Method to handle failures
    void onFailure() {
        failureCount_++;
        if (failureCount_ >= failureThreshold_) {
            state_ = CircuitState::OPEN;
            lastFailureTime_ = std::chrono::system_clock::now();
        }
    }

    // Method to handle successes
    void onSuccess() {
        if (state_ == CircuitState::HALF_OPEN) {
            successCount_++;
            if (successCount_ >= successThreshold_) {
                reset();
            }
        } else {
            failureCount_ = 0;  // Reset failure count in closed state
        }
    }

    // Reset the circuit breaker to the closed state
    void reset() {
        state_ = CircuitState::CLOSED;
        failureCount_ = 0;
        successCount_ = 0;
    }

    // Fallback handling when circuit is open
    std::string fallback() {
        return "Service is unavailable. Returning fallback response.";
    }
};

// Simulate external service
std::string externalService() {
    static int attempt = 0;
    attempt++;
    if (attempt % 3 != 0) {
        throw std::runtime_error("Service failed");
    }
    return "Service success";
}

// Example integration
int main() {
    CircuitBreaker cb(3, 5, 2);  // failureThreshold = 3, retryTime = 5 seconds, successThreshold = 2

    for (int i = 0; i < 10; ++i) {
        try {
            std::string result = cb.execute(externalService);
            std::cout << result << std::endl;
        } catch (const std::exception& e) {
            std::cerr << "Error: " << e.what() << std::endl;
        }

        auto metrics = cb.getMetrics();
        std::cout << "Failure count: " << metrics["failureCount"] << ", Success count: " << metrics["successCount"]
                  << ", Circuit state: " << (metrics["currentState"] == 0 ? "CLOSED" : metrics["currentState"] == 1 ? "OPEN" : "HALF_OPEN")
                  << std::endl;

        std::this_thread::sleep_for(std::chrono::seconds(1));
    }
}
