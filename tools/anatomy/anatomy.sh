#!/usr/bin/env bash
# Works on the anatomy figure without Gradle or Google Maven: compiles the pure Kotlin anatomy and
# exercise code with the Kotlin compiler from Maven Central, then runs its tests, renders exercises
# to a PNG or checks each exercise's camera. See docs/anatomy.md and .claude/skills/add-exercise.
#
#   tools/anatomy/anatomy.sh test                      unit tests for anatomy, programs, session mode and the coach
#   tools/anatomy/anatomy.sh render out.png [id ...]    contact sheet: one row per exercise
#   tools/anatomy/anatomy.sh views [id ...]             best camera per exercise vs. the stored one
#   tools/anatomy/anatomy.sh run Main.kt [args ...]     compile your own file (e.g. a pose solver) and run it
#
# The PNG is a preview drawn with Java2D, not the app: reference screenshots always come from CI.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
KOTLIN_VERSION="$(sed -n 's/^kotlin = "\(.*\)"/\1/p' "$ROOT/gradle/libs.versions.toml")"
JUNIT_VERSION="$(sed -n 's/^junit = "\(.*\)"/\1/p' "$ROOT/gradle/libs.versions.toml")"
COROUTINES_VERSION="$(sed -n 's/^kotlinxCoroutines = "\(.*\)"/\1/p' "$ROOT/gradle/libs.versions.toml")"
CACHE="${XDG_CACHE_HOME:-$HOME/.cache}/novila-anatomy/$KOTLIN_VERSION-$COROUTINES_VERSION"
LIB="$CACHE/lib"
OUT="$CACHE/classes"

fetch() {
  [ -f "$LIB/kotlin-stdlib-$KOTLIN_VERSION.jar" ] && return
  mkdir -p "$LIB"
  cat > "$CACHE/pom.xml" <<POM
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
<groupId>local</groupId><artifactId>anatomy-tools</artifactId><version>1</version>
<dependencies>
<dependency><groupId>org.jetbrains.kotlin</groupId><artifactId>kotlin-compiler-embeddable</artifactId><version>$KOTLIN_VERSION</version></dependency>
<dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>$JUNIT_VERSION</version></dependency>
<dependency><groupId>org.jetbrains.kotlinx</groupId><artifactId>kotlinx-coroutines-core-jvm</artifactId><version>$COROUTINES_VERSION</version></dependency>
</dependencies></project>
POM
  echo "Downloading Kotlin $KOTLIN_VERSION, coroutines and JUnit $JUNIT_VERSION from Maven Central..." >&2
  (cd "$CACHE" && mvn -q dependency:copy-dependencies -DoutputDirectory="$LIB")
}

# The plain Kotlin modules :core:anatomy, :core:exercise, :core:coach and what the coach builds on
# (:core:recovery, :core:model, :core:sleep), with no Android, so no Gradle needed.
sources() {
  find "$ROOT/core/anatomy/src/main" "$ROOT/core/exercise/src/main" "$ROOT/core/coach/src/main" \
    "$ROOT/core/recovery/src/main" "$ROOT/core/model/src/main" "$ROOT/core/sleep/src/main" -name '*.kt'
  find "$ROOT/tools/anatomy/src" -name '*.kt'
}

compile() {
  fetch
  rm -rf "$OUT" && mkdir -p "$OUT"
  local compiler
  compiler="$(find "$LIB" -name '*.jar' | tr '\n' ':')"
  java -cp "$compiler" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -nowarn \
    -cp "$(libraries)" -d "$OUT" "$@"
}

# The ExerciseRepository's StateFlow needs coroutines; the compiler jars stay off the classpath.
libraries() { echo "$LIB/kotlin-stdlib-$KOTLIN_VERSION.jar:$LIB/junit-$JUNIT_VERSION.jar:$LIB/kotlinx-coroutines-core-jvm-$COROUTINES_VERSION.jar"; }

classpath() { echo "$OUT:$(libraries):$(find "$LIB" -name 'hamcrest*.jar')"; }

command="${1:-test}"
shift || true
case "$command" in
  test)
    tests=(
      "$ROOT/core/anatomy/src/test/java/se/novila/anatomy/AnatomyTest.kt"
      "$ROOT/core/anatomy/src/test/java/se/novila/anatomy/RepetitionCounterTest.kt"
      "$ROOT/core/exercise/src/test/java/se/novila/exercise/StrengthSessionsTest.kt"
      "$ROOT/core/exercise/src/test/java/se/novila/exercise/TrainingProgramGeneratorTest.kt"
      "$ROOT/core/exercise/src/test/java/se/novila/exercise/WorkoutTest.kt"
      "$ROOT/core/exercise/src/test/java/se/novila/exercise/ExerciseRepositoryTest.kt"
      "$ROOT/core/exercise/src/test/java/se/novila/exercise/TrainingHistoryTest.kt"
      "$ROOT/core/coach/src/test/java/se/novila/coach/CoachFeedbackTest.kt"
      "$ROOT/core/coach/src/test/java/se/novila/coach/CoachPlannerTest.kt"
      "$ROOT/core/coach/src/test/java/se/novila/coach/CoachPreferencesTest.kt"
    )
    # shellcheck disable=SC2046
    compile $(sources) "${tests[@]}"
    java -cp "$(classpath)" org.junit.runner.JUnitCore \
      se.novila.anatomy.AnatomyTest se.novila.anatomy.RepetitionCounterTest \
      se.novila.exercise.StrengthSessionsTest se.novila.exercise.TrainingProgramGeneratorTest \
      se.novila.exercise.WorkoutTest se.novila.exercise.ExerciseRepositoryTest se.novila.exercise.TrainingHistoryTest \
      se.novila.coach.CoachPlannerTest se.novila.coach.CoachFeedbackTest se.novila.coach.CoachPreferencesTest
    ;;
  render)
    # shellcheck disable=SC2046
    compile $(sources)
    java -Djava.awt.headless=true -cp "$(classpath)" RenderKt "$@"
    ;;
  views)
    # shellcheck disable=SC2046
    compile $(sources)
    java -cp "$(classpath)" ViewsKt "$@"
    ;;
  run)
    main="$1"
    shift
    # shellcheck disable=SC2046
    compile $(sources) "$main"
    java -Djava.awt.headless=true -cp "$(classpath)" "$(basename "${main%.kt}")Kt" "$@"
    ;;
  *)
    sed -n '2,12p' "$0"
    exit 1
    ;;
esac
