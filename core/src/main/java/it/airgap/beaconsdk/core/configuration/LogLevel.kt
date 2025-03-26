package it.airgap.beaconsdk.core.configuration

public enum class LogLevel(internal val value: Int) {
    Off(0),
    Error(1),
    Info(2),
    Debug(3),
    ;

    public companion object {
        public val Default: LogLevel = Error
    }
}