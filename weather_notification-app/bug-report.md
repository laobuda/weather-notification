## [2026-07-18 10:00] Integration Tests Not Executing

**Failure Details:**
- **File Path:** `/home/laobuda/IdeaProjects/weather-notification/weather_notification-app/src/test/java/com/example/weather/NotificationFlowIT.java`
- **File Path:** `/home/laobuda/IdeaProjects/weather-notification/weather_notification-app/src/test/java/com/example/weather/WeatherApiFlowIT.java`
- **Root Cause:** The integration tests follow the `*IT.java` naming convention. By default, Maven's `surefire` plugin only executes tests matching `**/Test*.java`, `**/*Test.java`, `**/*Tests.java`, and `**/*TestCase.java`. Integration tests ending in `IT.java` are intended to be picked up by the `maven-failsafe-plugin`, which is currently missing from the `pom.xml`. As a result, `mvn verify` completes successfully but skips these tests entirely.

**Actionable Instructions for Developer Subagent:**
1. Open `/home/laobuda/IdeaProjects/weather-notification/weather_notification-app/pom.xml`.
2. Add the `maven-failsafe-plugin` to the `<plugins>` section within the `<build>` block.
3. Ensure the plugin configuration includes the standard lifecycle execution for `integration-test` and `verify` goals.
4. Run `mvn verify` to confirm that `NotificationFlowIT` and `WeatherApiFlowIT` are now executed.
