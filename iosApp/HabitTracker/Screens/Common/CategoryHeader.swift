import SwiftUI
import Shared

struct CategoryHeader: View {
    let category: Category_

    var body: some View {
        Text(category.name)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(Palette.color(category.colorKey))
            .textCase(nil)
    }
}
