package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Object delegate;
  private final Clock clock;
  private final ProfilingState state;

  ProfilingMethodInterceptor(Object delegate, Clock clock, ProfilingState state) {
    this.delegate = Objects.requireNonNull(delegate);
    this.clock = Objects.requireNonNull(clock);
    this.state = Objects.requireNonNull(state);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    boolean isProfiled = method.isAnnotationPresent(Profiled.class);
    Instant start = isProfiled ? clock.instant() : null;

    try {
      Object result = method.invoke(delegate, args);
      if (isProfiled) {
        Duration duration = Duration.between(start, clock.instant());
        state.record(delegate.getClass(), method, duration);
      }
      return result;
    } catch (Throwable t) {
      if (isProfiled) {
        Duration duration = Duration.between(start, clock.instant());
        state.record(delegate.getClass(), method, duration);
      }
      throw t.getCause();  // Re-throw the underlying exception thrown by the target method
    }
  }

}
