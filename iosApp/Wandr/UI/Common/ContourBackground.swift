import SwiftUI
@preconcurrency import shared

/// Elevation lines like on a topographic map as a quiet backdrop: the design language of WANDR for screens that are not
/// dominated by content (sign in, sign up, empty states). The lines come from the shared `ContourLines` (identical on
/// Android) and are drawn in a faint tone of the text color, so they work in light and dark mode; every fifth line is a
/// bit stronger, like the index contours of a map.
struct ContourBackground<Content: View>: View {
    var seed: Int32 = 7
    @ViewBuilder var content: () -> Content

    var body: some View {
        ZStack {
            Color(.systemBackground).ignoresSafeArea()
            GeometryReader { geo in
                let aspect = max(0.2, (geo.size.width / max(geo.size.height, 1) * 10).rounded() / 10)
                let contours = ContourCache.shared.paths(aspect: aspect, seed: seed)
                Canvas { context, size in
                    let scale = CGAffineTransform(scaleX: size.width, y: size.height)
                    context.stroke(contours.regular.applying(scale), with: .color(.primary.opacity(0.10)), lineWidth: 0.8)
                    context.stroke(contours.index.applying(scale), with: .color(.primary.opacity(0.22)), lineWidth: 1.4)
                }
            }
            .ignoresSafeArea()
            .allowsHitTesting(false)
            content()
        }
    }
}

/// The contour paths in unit coordinates (0...1), computed once per area shape.
final class ContourCache: @unchecked Sendable {
    static let shared = ContourCache()

    struct Paths {
        let regular: Path;
        let index: Path
    }

    private var cache: [String: Paths] = [:]
    private let lock = NSLock()
    private let indexEvery: Int32 = 5

    func paths(aspect: CGFloat, seed: Int32) -> Paths {
        let key = "\(aspect)-\(seed)"
        lock.lock(); defer {
            lock.unlock()
        }
        if let cached = cache[key] {
            return cached
        }

        let set = ContourLines.shared.generate(aspect: Float(aspect), seed: seed, columns: 44, levels: 18)
        var regular = Path()
        var index = Path()
        for i in 0..<Int(set.segmentCount) {
            let a = CGPoint(x: CGFloat(set.segments.get(index: Int32(i * 4))), y: CGFloat(set.segments.get(index: Int32(i * 4 + 1))))
            let b = CGPoint(x: CGFloat(set.segments.get(index: Int32(i * 4 + 2))), y: CGFloat(set.segments.get(index: Int32(i * 4 + 3))))
            if set.levels.get(index: Int32(i)) % indexEvery == 0 {
                index.move(to: a); index.addLine(to: b)
            } else {
                regular.move(to: a); regular.addLine(to: b)
            }
        }
        let result = Paths(regular: regular, index: index)
        cache[key] = result
        return result
    }
}

#Preview("Light Mode") {
    ContourBackground {
        Text("WANDR").font(.largeTitle.bold())
    }
}

#Preview("Dark Mode") {
    ContourBackground {
        Text("WANDR").font(.largeTitle.bold())
    }
    .preferredColorScheme(.dark)
}
