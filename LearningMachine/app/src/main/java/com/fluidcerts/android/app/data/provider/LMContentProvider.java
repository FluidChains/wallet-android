package com.fluidcerts.android.app.data.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.fluidcerts.android.app.data.inject.Injector;
import com.fluidcerts.android.app.data.store.LMDatabaseHelper;

import java.util.Objects;

import javax.inject.Inject;

import timber.log.Timber;

public class LMContentProvider extends ContentProvider {

    private static final String AUTHORITY = LMContentProviderConstants.CONTENT_AUTHORITY;

    public static final String BASE_PATH_ISSUER = LMContentProviderConstants.BASE_PATH_ISSUER;
    public static final String BASE_PATH_CERTIFICATE = LMContentProviderConstants.BASE_PATH_CERTIFICATE;

    private static final String QUERY = LMContentProviderConstants.QUERY;
    private static final String INSERT = LMContentProviderConstants.INSERT;
    private static final String UPDATE = LMContentProviderConstants.UPDATE;
    private static final String DELETE = LMContentProviderConstants.DELETE;

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/");
    public static final Uri CONTENT_URI_CERTIFICATES = Uri.parse(CONTENT_URI + BASE_PATH_ISSUER);

    public static final Uri CONTENT_URI_QUERY_ISSUERS = Uri.parse(CONTENT_URI + QUERY + "/" + BASE_PATH_ISSUER);
    public static final Uri CONTENT_URI_INSERT_ISSUERS = Uri.parse(CONTENT_URI + INSERT + "/" + BASE_PATH_ISSUER);
    public static final Uri CONTENT_URI_UPDATE_ISSUERS = Uri.parse(CONTENT_URI + UPDATE + "/" + BASE_PATH_ISSUER);
    public static final Uri CONTENT_URI_DELETE_ISSUERS = Uri.parse(CONTENT_URI + DELETE + "/" + BASE_PATH_ISSUER);

    public static final Uri CONTENT_URI_QUERY_CERTIFICATES = Uri.parse(CONTENT_URI + QUERY + "/" + BASE_PATH_CERTIFICATE);
    public static final Uri CONTENT_URI_INSERT_CERTIFICATES = Uri.parse(CONTENT_URI + INSERT + "/" + BASE_PATH_CERTIFICATE);
    public static final Uri CONTENT_URI_UPDATE_CERTIFICATES = Uri.parse(CONTENT_URI + UPDATE + "/" + BASE_PATH_CERTIFICATE);
    public static final Uri CONTENT_URI_DELETE_CERTIFICATES = Uri.parse(CONTENT_URI + DELETE + "/" + BASE_PATH_CERTIFICATE);

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int ISSUERS = 1;
    private static final int ISSUER_ID = 2;
    private static final int CERTIFICATES = 3;
    private static final int CERTIFICATE_ID = 4;

    @Inject SQLiteDatabase mDatabase;

    static {
        uriMatcher.addURI(AUTHORITY, "*/" + BASE_PATH_ISSUER, 1);
        uriMatcher.addURI(AUTHORITY, "*/" + BASE_PATH_ISSUER + "/#", 2);
        uriMatcher.addURI(AUTHORITY, "*/" + BASE_PATH_CERTIFICATE, 3);
        uriMatcher.addURI(AUTHORITY, "*/" + BASE_PATH_CERTIFICATE + "/#", 4);
    }

    private void init() {
        if (mDatabase == null) {
            Injector.obtain(getContext())
                    .inject(this);
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case ISSUERS:
                return "vnd.android.cursor.dir/" + AUTHORITY + BASE_PATH_ISSUER;
            case ISSUER_ID:
                return "vnd.android.cursor.item/" + AUTHORITY + BASE_PATH_ISSUER;
            case CERTIFICATES:
                return "vnd.android.cursor.dir/" + AUTHORITY + BASE_PATH_CERTIFICATE;
            case CERTIFICATE_ID:
                return "vnd.android.cursor.item/" + AUTHORITY + BASE_PATH_CERTIFICATE;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        init();
        switch (uriMatcher.match(uri)) {
            case ISSUERS:
            case CERTIFICATES:
                if (TextUtils.isEmpty(sortOrder)) sortOrder = "id ASC";
                break;
            case ISSUER_ID:
            case CERTIFICATE_ID:
                selection = selection + "id = " + uri.getLastPathSegment();
                break;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
        String table;
        switch (uriMatcher.match(uri)) {
            case CERTIFICATES:
            case CERTIFICATE_ID:
                table = LMDatabaseHelper.Table.CERTIFICATE;
                break;
            case ISSUERS:
            case ISSUER_ID:
                table = LMDatabaseHelper.Table.ISSUER;
                break;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }

        Cursor cursor = mDatabase.query(table, projection,
                selection, selectionArgs, null, null, sortOrder);

        Context context = getContext();
        if(context != null) {
            ContentResolver resolver = context.getContentResolver();
            cursor.setNotificationUri(resolver, uri);
        }

        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        init();
        String table;
        Uri contentUri;
        switch (uriMatcher.match(uri)) {
            case CERTIFICATES:
            case CERTIFICATE_ID:
                table = LMDatabaseHelper.Table.CERTIFICATE;
                contentUri = CONTENT_URI_INSERT_CERTIFICATES;
                break;
            case ISSUERS:
            case ISSUER_ID:
                table = LMDatabaseHelper.Table.ISSUER;
                contentUri = CONTENT_URI_INSERT_ISSUERS;
                break;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
        long _ID = mDatabase.insert(table, null, contentValues);
        Timber.d(String.format("Content Provider inserting %s/%s", uri.toString(), _ID));

        if (_ID > 0) {
            Uri _uri = ContentUris.withAppendedId(contentUri, _ID);
            Objects.requireNonNull(getContext()).getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        throw new SQLException("Insertion Failed for URI :" + uri);
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        init();
        String table;
        Uri contentUri;
        switch (uriMatcher.match(uri)) {
            case CERTIFICATES:
            case CERTIFICATE_ID:
                table = LMDatabaseHelper.Table.CERTIFICATE;
                contentUri = CONTENT_URI_INSERT_CERTIFICATES;
                break;
            case ISSUERS:
            case ISSUER_ID:
                table = LMDatabaseHelper.Table.ISSUER;
                contentUri = CONTENT_URI_INSERT_ISSUERS;
                break;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }

        int rowsAffected = 0;
        try {
            mDatabase.beginTransaction();
            for (ContentValues contentValues: values) {
                long _id = mDatabase.insert(table, null, contentValues);
                Timber.d(String.format("Content Provider inserting %s/%s", uri.toString(), _id));

                if (_id > 0) {
                    rowsAffected += 1;
                } else {
                    throw new SQLException(String.format("Content Provider insertion failed for %s/%s", uri.toString(), _id));
                }

            }
            mDatabase.setTransactionSuccessful();
            getContext().getContentResolver().notifyChange(contentUri, null, true);
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            mDatabase.endTransaction();
        }
        return rowsAffected;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        init();
        String id;
        int rowsAffected = 0;
        switch (uriMatcher.match(uri)){
            case ISSUERS:
                rowsAffected = mDatabase.delete(LMDatabaseHelper.Table.ISSUER, selection, selectionArgs);
                break;
            case ISSUER_ID:
                id = uri.getPathSegments().get(1);
                rowsAffected = mDatabase.delete(LMDatabaseHelper.Table.ISSUER, "_id = " + id +
                        (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : ""), selectionArgs);
                break;
            case CERTIFICATES:
                rowsAffected = mDatabase.delete(LMDatabaseHelper.Table.CERTIFICATE, selection, selectionArgs);
                break;
            case CERTIFICATE_ID:
                id = uri.getPathSegments().get(1);
                rowsAffected = mDatabase.delete(LMDatabaseHelper.Table.CERTIFICATE, "_id = " + id +
                        (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        Objects.requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);
        return rowsAffected;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {
        init();
        int rowsAffected = 0;
        switch (uriMatcher.match(uri)) {
            case ISSUERS:
                rowsAffected = mDatabase.update(LMDatabaseHelper.Table.ISSUER, contentValues, selection, selectionArgs);
                break;
            case ISSUER_ID:
                rowsAffected = mDatabase.update(LMDatabaseHelper.Table.ISSUER, contentValues,
                        "_id = " + uri.getPathSegments().get(1) + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : ""), selectionArgs);
                break;
            case CERTIFICATES:
                rowsAffected = mDatabase.update(LMDatabaseHelper.Table.CERTIFICATE, contentValues, selection, selectionArgs);
                break;
            case CERTIFICATE_ID:
                rowsAffected = mDatabase.update(LMDatabaseHelper.Table.CERTIFICATE, contentValues,
                        "_id = " + uri.getPathSegments().get(1) + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
        Objects.requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);
        return rowsAffected;
    }
}
