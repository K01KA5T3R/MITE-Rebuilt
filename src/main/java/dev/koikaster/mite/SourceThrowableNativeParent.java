package dev.koikaster.mite;
/** Direct Entity tick, supplied on native Projectile. Only the new uu owner
 * calls this bridge; other projectiles keep their normal tick path. */
public interface SourceThrowableNativeParent {void mite$throwableEntityTick();}
