package jnr.posix;

import jnr.constants.platform.Errno;
import jnr.constants.platform.PosixFadvise;
import jnr.constants.platform.Sysconf;
import jnr.ffi.Memory;
import jnr.ffi.Pointer;
import jnr.ffi.mapper.FromNativeContext;
import jnr.posix.util.Platform;

import java.io.FileDescriptor;
import java.nio.ByteBuffer;
import jnr.constants.platform.Confstr;
import jnr.constants.platform.Pathconf;

final class LinuxPOSIX extends BaseNativePOSIX implements Linux {
    private final boolean use_stat64;
    private final int statVersion;

    LinuxPOSIX(LibCProvider libcProvider, POSIXHandler handler) {
        super(libcProvider, handler);

        statVersion = getStatVersion();
        use_stat64 = statVersion >= 0;
    }

    private int getStatVersion() {
        if (Platform.IS_32_BIT || "sparcv9".equals(Platform.ARCH) || Platform.ARCH.contains("mips64")) {
            return 3;
        } else {
            FileStat stat = allocateStat();
            try {
                if (((LinuxLibC) libc()).__xstat64(0, "/dev/null", stat) < 0) {
                    return 1;
                }
                return 0;
            } catch (UnsatisfiedLinkError ex) {
                return -1;
            }
        }
    }

    @Override
    public FileStat allocateStat() {
        if (Platform.IS_32_BIT) {
            return new LinuxFileStat32(this);
        } else {
            if ("aarch64".equals(Platform.ARCH)) {
                return new LinuxFileStatAARCH64(this);
            } else if ("riscv64".equals(Platform.ARCH)) {
                return new LinuxFileStatRISCV64(this);
            } else if ("sparcv9".equals(Platform.ARCH)) {
		return new LinuxFileStatSPARCV9(this);
	    } else if ("loongarch64".equals(Platform.ARCH)) {
		return new LinuxFileStatLOONGARCH64(this);
	    } else {
		if (Platform.ARCH.contains("mips64")) {
		    return new LinuxFileStatMIPS64(this);
		}
                return new LinuxFileStat64(this);
	    }
	}
    }

    public MsgHdr allocateMsgHdr() {
        return new LinuxMsgHdr(this);
    }

    @Override
    public Pointer allocatePosixSpawnFileActions() {
        return Memory.allocateDirect(getRuntime(), 80);
    }

    @Override
    public Pointer allocatePosixSpawnattr() {
        return Memory.allocateDirect(getRuntime(), 336);
    }

    public SocketMacros socketMacros() {
        return LinuxSocketMacros.INSTANCE;
    }

    private int old_fstat(int fd, FileStat stat) {
        try {
            return super.fstat(fd, stat);
        } catch (UnsatisfiedLinkError ex2) {
            handler.unimplementedError("fstat");
            return -1;
        }
    }

    @Override
    public int fstat(int fd, FileStat stat) {
        if (use_stat64) {
            return ((LinuxLibC) libc()).__fxstat64(statVersion, fd, stat);
        } else {
            return old_fstat(fd, stat);
        }
    }

    @Override
    public FileStat fstat(int fd) {
        FileStat stat = allocateStat();
        int ret = fstat(fd, stat);
        if (ret < 0) handler.error(Errno.valueOf(errno()), "fstat", Integer.toString(fd));
        return stat;
    }

    @Override
    public int fstat(FileDescriptor fileDescriptor, FileStat stat) {
        return fstat(helper.getfd(fileDescriptor), stat);
    }

    @Override
    public FileStat fstat(FileDescriptor fileDescriptor) {
        FileStat stat = allocateStat();
        int fd = helper.getfd(fileDescriptor);
        int ret = fstat(fd, stat);
        if (ret < 0) handler.error(Errno.valueOf(errno()), "fstat", Integer.toString(fd));
        return stat;
    }

    private final int old_lstat(String path, FileStat stat) {
        try {
            return super.lstat(path, stat);
        } catch (UnsatisfiedLinkError ex) {
            handler.unimplementedError("lstat");
            return -1;
        }
    }

    @Override
    public int lstat(String path, FileStat stat) {
        if (use_stat64) {
            return ((LinuxLibC) libc()).__lxstat64(statVersion, path, stat);
        } else {
            return old_lstat(path, stat);
        }
    }

    @Override
    public FileStat lstat(String path) {
        FileStat stat = allocateStat();
        int ret = lstat(path, stat);
        if (ret < 0) handler.error(Errno.valueOf(errno()), "lstat", path);
        return stat;
    }

    private final int old_stat(String path, FileStat stat) {
        try {
            return super.stat(path, stat);
        } catch (UnsatisfiedLinkError ex) {
            handler.unimplementedError("stat");
            return -1;
        }
    }

    @Override
    public int stat(String path, FileStat stat) {

        if (use_stat64) {
            return ((LinuxLibC) libc()).__xstat64(statVersion, path, stat);
        } else {
            return old_stat(path, stat);
        }
    }

    @Override
    public FileStat stat(String path) {
        FileStat stat = allocateStat();
        int ret = stat(path, stat);
        if (ret < 0) handler.error(Errno.valueOf(errno()), "stat", path);
        return stat;
    }

    public long sysconf(Sysconf name) {
        return libc().sysconf(name);
    }

    public int confstr(Confstr name, ByteBuffer buf, int len) {
        return libc().confstr(name, buf, len);
    }

    public int fpathconf(int fd, Pathconf name) {
        return libc().fpathconf(fd, name);
    }

    public Times times() {
        return NativeTimes.times(this);
    }

    public static final PointerConverter PASSWD = new PointerConverter() {
        public Object fromNative(Object arg, FromNativeContext ctx) {
            return arg != null ? new LinuxPasswd((Pointer) arg) : null;
        }
    };

    static final public class Syscall {
        static final ABI _ABI_X86_32 = new ABI_X86_32();
        static final ABI _ABI_X86_64 = new ABI_X86_64();
        static final ABI _ABI_AARCH64 = new ABI_AARCH64();
        static final ABI _ABI_SPARCV9 = new ABI_SPARCV9();
        static final ABI _ABI_PPC64 = new ABI_PPC64();
        static final ABI _ABI_MIPS64 = new ABI_MIPS64();
        static final ABI _ABI_LOONGARCH64 = new ABI_LOONGARCH64();
	static final ABI _ABI_RISCV64 = new ABI_RISCV64();

        public static ABI abi() {
            if ("x86_64".equals(Platform.ARCH)) {
                if (Platform.IS_64_BIT) {
                    return _ABI_X86_64;
                }
            } else if ("i386".equals(Platform.ARCH)) {
                return _ABI_X86_32;
            } else if ("aarch64".equals(Platform.ARCH)) {
                return _ABI_AARCH64;
            } else if ("sparcv9".equals(Platform.ARCH)) {
                return _ABI_SPARCV9;
            } else if (Platform.ARCH.contains("ppc64")) {
                return _ABI_PPC64;
            } else if (Platform.ARCH.contains("mips64")) {
	    	return _ABI_MIPS64;
	    } else if (Platform.ARCH.contains("loongarch64")) {
		return _ABI_LOONGARCH64;
	    } else if (Platform.ARCH.contains("riscv64")) {
                return _ABI_RISCV64;
	    }
            return null;
        }

        interface ABI {
            public int __NR_ioprio_set();
            public int __NR_ioprio_get();
        }

        /** @see /usr/include/asm/unistd_32.h */
        final static class ABI_X86_32 implements ABI {
            @Override
            public int __NR_ioprio_set() {
                return 289;
            }
            @Override
            public int __NR_ioprio_get() {
                return 290;
            }
        }

        /** @see /usr/include/asm/unistd_64.h */
        final static class ABI_X86_64 implements ABI {
            @Override
            public int __NR_ioprio_set() {
                return 251;
            }
            @Override
            public int __NR_ioprio_get() {
                return 252;
            }
        }

        /** @see /usr/include/asm-generic/unistd.h */
        final static class ABI_AARCH64 implements ABI {
            @Override
            public int __NR_ioprio_set() {
                return 30;
            }
            @Override
            public int __NR_ioprio_get() {
                return 31 ;
            }
        }

        /** @see /usr/include/asm/unistd.h */
        final static class ABI_SPARCV9 implements ABI {
            @Override
            public int __NR_ioprio_set() {
                return 196;
            }
            @Override
            public int __NR_ioprio_get() {
                return 218;
            }
        }

        /** @see /usr/include/asm-generic/unistd.h */
        final static class ABI_PPC64 implements ABI {
            @Override
            public int __NR_ioprio_set() {
                return 273;
            }
            @Override
            public int __NR_ioprio_get() {
                return 274 ;
            }
        }

        /** @see /usr/include/asm/unistd.h */
        final static class ABI_MIPS64 implements ABI {
            @Override
            public int __NR_ioprio_set() {
                return 5273;
            }
            @Override
            public int __NR_ioprio_get() {
                return 5274;
            }
        }

        /** @see /usr/include/asm-generic/unistd.h */
        final static class ABI_LOONGARCH64 implements ABI {
            @Override
            public int __NR_ioprio_set() {
                return 30;
            }
            @Override
            public int __NR_ioprio_get() {
                return 31;
            }
        }
	    
	/** @see /usr/include/asm-generic/unistd.h */
        final static class ABI_RISCV64 implements ABI {
            @Override
            public int __NR_ioprio_set() {
                return 30;
            }
            @Override
            public int __NR_ioprio_get() {
                return 31 ;
            }
        }
    }


    public int ioprio_get(int which, int who) {
        Syscall.ABI abi = Syscall.abi();
        if (abi == null) {
            handler.unimplementedError("ioprio_get");
            return -1;
        }

        return libc().syscall(abi.__NR_ioprio_get(), which, who);
    }

    public int ioprio_set(int which, int who, int ioprio) {
        Syscall.ABI abi = Syscall.abi();
        if (abi == null) {
            handler.unimplementedError("ioprio_set");
            return -1;
        }

        return libc().syscall(abi.__NR_ioprio_set(), which, who, ioprio);
    }

    public int posix_fadvise(int fd, long offset, long len, PosixFadvise advise) {
        return ((LinuxLibC) libc()).posix_fadvise(fd, offset, len, advise.intValue());
    }
}
