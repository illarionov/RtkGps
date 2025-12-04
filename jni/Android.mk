JNI_TOP_PATH:= $(call my-dir)
LOCAL_PATH := $(call my-dir)

TARGET_PLATFORM := android-21

include $(CLEAR_VARS)
include $(LOCAL_PATH)/gdal.mk

include $(LOCAL_PATH)/rtklib.mk

include $(CLEAR_VARS)

LOCAL_MODULE    := rtkgps
LOCAL_CFLAGS += -fvisibility=hidden
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../jni/RTKLIB/src
LOCAL_SRC_FILES := \
	gtime.c \
	prcopt.c \
	rtkjni.c \
	rtkcommon.c \
	rtkserver.c \
	solopt.c

LOCAL_STATIC_LIBRARIES := rtklib

include $(BUILD_SHARED_LIBRARY)

#Build proj4
include $(CLEAR_VARS)
LOCAL_MODULE    := proj
LOCAL_C_INCLUDES := $(LOCAL_PATH)/proj4/src
LOCAL_CFLAGS    := -DJNI_ENABLED=1
LOCAL_LDLIBS := -lm
LOCAL_LDLIBS += -llog
LOCAL_SRC_FILES := \
    proj4/src/pj_list.h proj4/src/proj_internal.h proj4/src/proj_math.h\
    proj4/src/PJ_aeqd.c proj4/src/PJ_gnom.c proj4/src/PJ_laea.c proj4/src/PJ_mod_ster.c \
    proj4/src/PJ_nsper.c proj4/src/PJ_nzmg.c proj4/src/PJ_ortho.c proj4/src/PJ_stere.c proj4/src/PJ_sterea.c \
    proj4/src/PJ_aea.c proj4/src/PJ_bipc.c proj4/src/PJ_bonne.c proj4/src/PJ_eqdc.c proj4/src/PJ_isea.c proj4/src/PJ_ccon.c\
    proj4/src/PJ_imw_p.c proj4/src/PJ_krovak.c proj4/src/PJ_lcc.c proj4/src/PJ_poly.c \
    proj4/src/PJ_rpoly.c proj4/src/PJ_sconics.c proj4/src/proj_rouss.c \
    proj4/src/PJ_cass.c proj4/src/PJ_cc.c proj4/src/PJ_cea.c proj4/src/PJ_eqc.c proj4/src/PJ_gall.c proj4/src/PJ_geoc.c \
    proj4/src/PJ_labrd.c proj4/src/PJ_lsat.c proj4/src/PJ_misrsom.c proj4/src/PJ_merc.c \
    proj4/src/PJ_mill.c proj4/src/PJ_ocea.c proj4/src/PJ_omerc.c proj4/src/PJ_somerc.c \
    proj4/src/PJ_tcc.c proj4/src/PJ_tcea.c proj4/src/PJ_times.c proj4/src/PJ_tmerc.c \
    proj4/src/PJ_airy.c proj4/src/PJ_aitoff.c proj4/src/PJ_august.c proj4/src/PJ_bacon.c \
    proj4/src/PJ_chamb.c proj4/src/PJ_hammer.c proj4/src/PJ_lagrng.c proj4/src/PJ_larr.c \
    proj4/src/PJ_lask.c proj4/src/PJ_latlong.c proj4/src/PJ_nocol.c proj4/src/PJ_ob_tran.c proj4/src/PJ_oea.c \
    proj4/src/PJ_tpeqd.c proj4/src/PJ_vandg.c proj4/src/PJ_vandg2.c proj4/src/PJ_vandg4.c \
    proj4/src/PJ_wag7.c proj4/src/PJ_lcca.c proj4/src/PJ_geos.c proj4/src/proj_etmerc.c \
    proj4/src/PJ_boggs.c proj4/src/PJ_collg.c proj4/src/PJ_comill.c proj4/src/PJ_crast.c proj4/src/PJ_denoy.c \
    proj4/src/PJ_eck1.c proj4/src/PJ_eck2.c proj4/src/PJ_eck3.c proj4/src/PJ_eck4.c \
    proj4/src/PJ_eck5.c proj4/src/PJ_fahey.c proj4/src/PJ_fouc_s.c proj4/src/PJ_gins8.c proj4/src/PJ_gstmerc.c \
    proj4/src/PJ_gn_sinu.c proj4/src/PJ_goode.c proj4/src/PJ_igh.c proj4/src/PJ_hatano.c proj4/src/PJ_loxim.c \
    proj4/src/PJ_mbt_fps.c proj4/src/PJ_mbtfpp.c proj4/src/PJ_mbtfpq.c proj4/src/PJ_moll.c \
    proj4/src/PJ_nell.c proj4/src/PJ_nell_h.c proj4/src/PJ_patterson.c proj4/src/PJ_putp2.c proj4/src/PJ_putp3.c \
    proj4/src/PJ_putp4p.c proj4/src/PJ_putp5.c proj4/src/PJ_putp6.c proj4/src/PJ_qsc.c proj4/src/PJ_robin.c \
    proj4/src/PJ_sch.c proj4/src/PJ_sts.c proj4/src/PJ_urm5.c proj4/src/PJ_urmfps.c proj4/src/PJ_wag2.c \
    proj4/src/PJ_wag3.c proj4/src/PJ_wink1.c proj4/src/PJ_wink2.c proj4/src/pj_geocent.c \
    proj4/src/aasincos.c proj4/src/adjlon.c proj4/src/bch2bps.c proj4/src/bchgen.c \
    proj4/src/biveval.c proj4/src/dmstor.c proj4/src/mk_cheby.c proj4/src/pj_auth.c \
    proj4/src/pj_deriv.c proj4/src/pj_ell_set.c proj4/src/pj_ellps.c proj4/src/pj_errno.c \
    proj4/src/pj_factors.c proj4/src/pj_fwd.c proj4/src/pj_init.c proj4/src/pj_inv.c \
    proj4/src/pj_list.c proj4/src/pj_malloc.c proj4/src/pj_mlfn.c proj4/src/pj_msfn.c proj4/src/proj_mdist.c \
    proj4/src/pj_open_lib.c proj4/src/pj_param.c proj4/src/pj_phi2.c proj4/src/pj_pr_list.c \
    proj4/src/pj_qsfn.c proj4/src/pj_strerrno.c \
    proj4/src/pj_tsfn.c proj4/src/pj_units.c proj4/src/pj_ctx.c proj4/src/pj_log.c proj4/src/pj_zpoly1.c proj4/src/rtodms.c \
    proj4/src/vector1.c proj4/src/pj_release.c proj4/src/pj_gauss.c \
    proj4/src/PJ_healpix.c proj4/src/PJ_natearth.c proj4/src/PJ_natearth2.c proj4/src/PJ_calcofi.c proj4/src/pj_fileapi.c \
    \
    proj4/src/pj_gc_reader.c proj4/src/pj_gridcatalog.c \
    proj4/src/nad_cvt.c proj4/src/nad_init.c proj4/src/nad_intr.c proj4/src/emess.c proj4/src/emess.h \
    proj4/src/pj_apply_gridshift.c proj4/src/pj_datums.c proj4/src/pj_datum_set.c proj4/src/pj_transform.c \
    proj4/src/geocent.c proj4/src/geocent.h proj4/src/pj_utils.c proj4/src/pj_gridinfo.c proj4/src/pj_gridlist.c \
    proj4/src/jniproj.c proj4/src/pj_mutex.c proj4/src/pj_initcache.c proj4/src/pj_apply_vgridshift.c proj4/src/geodesic.c \
    proj4/src/pj_strtod.c proj4/src/pj_math.c\
    \
    proj4/src/proj_4D_api.c proj4/src/PJ_cart.c proj4/src/PJ_pipeline.c proj4/src/PJ_horner.c proj4/src/PJ_helmert.c \
    proj4/src/PJ_vgridshift.c proj4/src/PJ_hgridshift.c proj4/src/PJ_unitconvert.c proj4/src/PJ_molodensky.c \
    proj4/src/PJ_deformation.c proj4/src/pj_internal.c proj4/src/PJ_axisswap.c
include $(BUILD_SHARED_LIBRARY)

#Build ntripcaster
include $(CLEAR_VARS)
LOCAL_MODULE    := ntripcaster
LOCAL_C_INCLUDES := $(LOCAL_PATH)/ntripcaster/src
LOCAL_CFLAGS    := -DHAVE_CONFIG_H=1 -DJNI_ENABLED=1
LOCAL_LDLIBS := -lm
LOCAL_LDLIBS += -llog
LOCAL_SRC_FILES := \
	ntripcaster/src/avl.c \
	ntripcaster/src/client.c \
	ntripcaster/src/connection.c \
	ntripcaster/src/log.c \
	ntripcaster/src/main.c \
	ntripcaster/src/ntrip_string.c \
	ntripcaster/src/sock.c \
	ntripcaster/src/source.c \
	ntripcaster/src/threads.c \
	ntripcaster/src/timer.c \
	ntripcaster/src/utility.c \
	ntripcaster/src/ntripcaster_jni.c
include $(BUILD_SHARED_LIBRARY)
