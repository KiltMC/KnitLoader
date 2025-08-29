package xyz.bluspring.knit.loader.mod

/**
 * Stores information about the mod that Knit can store and other loaders to use.
 */
abstract class KnitMod(val definition: ModDefinition) {
    override fun toString(): String {
        return "${this.javaClass.simpleName}[id=${definition.id},name=${definition.displayName}]"
    }
}