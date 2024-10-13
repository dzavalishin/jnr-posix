package jnr.posix;

import jnr.ffi.StructLayout;
import jnr.posix.util.Platform;

public final class LinuxFileStatRISCV64 extends BaseFileStat implements NanosecondFileStat {
    public static final class Layout extends StructLayout {

        public Layout(jnr.ffi.Runtime runtime) {
            super(runtime);
        }

        public final dev_t st_dev = new dev_t();
        public final ino_t st_ino = new ino_t();
        public final mode_t st_mode = new mode_t();
        public final nlink_t st_nlink = new nlink_t();
        public final uid_t st_uid = new uid_t();
        public final gid_t st_gid = new gid_t();
        public final dev_t st_rdev = new dev_t();
        public final dev_t __pad1 = new dev_t();
        public final off_t st_size = new off_t();
        public final blksize_t st_blksize = new blksize_t();
        public final Signed32 __pad2 = new Signed32();
        public final blkcnt_t st_blocks = new blkcnt_t();
        public final time_t st_atime = new time_t();             // Time of last access
        public final SignedLong st_atimensec = new SignedLong(); // Time of last access (nanoseconds)
        public final time_t st_mtime = new time_t();             // Last data modification time
        public final SignedLong st_mtimensec = new SignedLong(); // Last data modification time (nanoseconds)
        public final time_t st_ctime = new time_t();             // Time of last status change
        public final SignedLong st_ctimensec = new SignedLong(); // Time of last status change (nanoseconds)
        public final Signed32 __unused4 = new Signed32();
        public final Signed32 __unused5 = new Signed32();
    }

    private static final Layout layout = new Layout(jnr.ffi.Runtime.getSystemRuntime());

    public LinuxFileStatRISCV64(LinuxPOSIX posix) {
        super(posix, layout);
    }

    public long atime() {
        return layout.st_atime.get(memory);
    }

    public long aTimeNanoSecs() {
        return layout.st_atimensec.get(memory);
    }

    public long blockSize() {
        return layout.st_blksize.get(memory);
    }

    public long blocks() {
        return layout.st_blocks.get(memory);
    }

    public long ctime() {
        return layout.st_ctime.get(memory);
    }

    public long cTimeNanoSecs() {
        return layout.st_ctimensec.get(memory);
    }

    public long dev() {
        return layout.st_dev.get(memory);
    }

    public int gid() {
        return (int) layout.st_gid.get(memory);
    }

    public long ino() {
        return layout.st_ino.get(memory);
    }

    public int mode() {
        return (int) layout.st_mode.get(memory);
    }

    public long mtime() {
        return layout.st_mtime.get(memory);
    }

    public long mTimeNanoSecs() {
        return layout.st_mtimensec.get(memory);
    }

    public int nlink() {
        return (int) layout.st_nlink.get(memory);
    }

    public long rdev() {
        return layout.st_rdev.get(memory);
    }

    public long st_size() {
        return layout. st_size.get(memory);
    }

    public int uid() {
        return (int) layout.st_uid.get(memory);
    }
}
