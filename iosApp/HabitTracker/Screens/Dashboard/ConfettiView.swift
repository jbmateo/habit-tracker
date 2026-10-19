import SwiftUI

/// A short confetti burst from near the top of the screen, played each time `trigger` changes.
/// Purely decorative: never takes touches and is hidden from VoiceOver.
struct ConfettiView: View {
    let trigger: Int
    let colors: [Color]

    private struct Piece {
        let x: CGFloat, vx: CGFloat, vy: CGFloat, spin: Double, size: CGFloat, color: Color
    }

    private static let duration: TimeInterval = 1.8
    private static let gravity: CGFloat = 900

    @State private var start: Date?
    @State private var pieces: [Piece] = []

    var body: some View {
        TimelineView(.animation(paused: start == nil)) { context in
            Canvas { canvas, size in
                guard let start else { return }
                let t = context.date.timeIntervalSince(start)
                guard t < Self.duration else { return }
                let fade = 1 - t / Self.duration
                for piece in pieces {
                    let x = piece.x * size.width + piece.vx * t
                    let y = 140 + piece.vy * t + Self.gravity * t * t / 2
                    var copy = canvas
                    copy.opacity = fade
                    copy.translateBy(x: x, y: y)
                    copy.rotate(by: .radians(piece.spin * t))
                    let rect = CGRect(x: -piece.size / 2, y: -piece.size / 4, width: piece.size, height: piece.size / 2)
                    copy.fill(Path(roundedRect: rect, cornerRadius: 1.5), with: .color(piece.color))
                }
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
        .onChange(of: trigger) { burst() }
        .onDisappear { start = nil }
    }

    private func burst() {
        let palette = colors.isEmpty ? [Theme.flame, Theme.gold] : colors
        pieces = (0..<60).map { _ in
            Piece(
                x: .random(in: 0.25...0.75),
                vx: .random(in: -260...260),
                vy: .random(in: -620 ... -260),
                spin: .random(in: -12...12),
                size: .random(in: 7...12),
                color: palette.randomElement()!
            )
        }
        let started = Date()
        start = started
        DispatchQueue.main.asyncAfter(deadline: .now() + Self.duration + 0.1) {
            if start == started { start = nil }
        }
    }
}
