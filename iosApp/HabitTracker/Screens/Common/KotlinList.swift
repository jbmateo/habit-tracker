import Foundation

/// Converts a Kotlin `List<T>` (bridged as `[Any]`) to `[T]`. An element of the wrong type is a programming error:
/// it trips `assertionFailure` in debug builds and is dropped in release builds rather than crashing.
func kotlinList<T>(_ items: [Any], as type: T.Type = T.self) -> [T] {
    let result = items.compactMap { $0 as? T }
    if result.count != items.count {
        assertionFailure("Dropped \(items.count - result.count) non-\(T.self) elements")
    }
    return result
}
