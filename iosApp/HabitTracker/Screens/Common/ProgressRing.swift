import SwiftUI

/// Ring showing `done` of `due`. Bumping `celebrateTrigger` plays a spring scale pulse.
struct ProgressRing: View {
    let done: Int
    let due: Int
    let celebrateTrigger: Int
    var size: CGFloat = 96

    @State private var scale: CGFloat = 1

    private var progress: Double { due == 0 ? 0 : min(1, Double(done) / Double(due)) }

    var body: some View {
        ZStack {
            Circle().stroke(.fill.tertiary, style: StrokeStyle(lineWidth: size * 0.12, lineCap: .round))
            Circle()
                .trim(from: 0, to: progress)
                .stroke(Color.accentColor, style: StrokeStyle(lineWidth: size * 0.12, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.easeOut(duration: 0.4), value: progress)
            Text("\(done)/\(due)").font(.headline.bold()).monospacedDigit()
        }
        .frame(width: size, height: size)
        .scaleEffect(scale)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(done) of \(due) habits done")
        .onChange(of: celebrateTrigger) {
            withAnimation(.spring(duration: 0.25, bounce: 0.5)) { scale = 1.2 }
            withAnimation(.spring(duration: 0.4, bounce: 0.4).delay(0.2)) { scale = 1 }
        }
    }
}
