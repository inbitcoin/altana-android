package com.greenaddress.greenapi.model;

import android.util.Log;
import android.util.SparseArray;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.SubaccountData;

import java.io.IOException;
import java.util.List;
import java.util.Observable;

public class SubaccountDataObservable extends Observable {
    private List<SubaccountData> mSubaccountData;
    private GDKSession mSession;
    private ListeningExecutorService mExecutor;
    private SparseArray<TransactionDataObservable> mTransactionDataObservables = new SparseArray<>();
    private SparseArray<TransactionDataObservable> mUTXODataObservables = new SparseArray<>();
    private SparseArray<ReceiveAddressObservable> mReceiveAddressObservables = new SparseArray<>();
    private SparseArray<BalanceDataObservable> mBalanceDataObservables = new SparseArray<>();
    private ActiveAccountObservable mActiveAccountObservable;
    private BlockchainHeightObservable mBlockchainHeightObservable;

    private SubaccountDataObservable() {}

    public SubaccountDataObservable(final GDKSession session,
                                    final ListeningExecutorService executor,
                                    final Model model) {
        mSession = session;
        mExecutor = executor;
        mTransactionDataObservables = model.getTransactionDataObservables();
        mBalanceDataObservables = model.getBalanceDataObservables();
        mReceiveAddressObservables = model.getReceiveAddressObservables();
        mActiveAccountObservable = model.getActiveAccountObservable();
        mBlockchainHeightObservable = model.getBlockchainHeightObservable();
        mUTXODataObservables = model.getUTXODataObservables();
        refresh();
    }

    public void refresh() {
        // this call is syncronous, because other observables depends on this to be initialized
        try {
            final long millis=System.currentTimeMillis();
            final List<SubaccountData> subAccounts = mSession.getSubAccounts();
            for (SubaccountData subAccount : subAccounts) {
                final int pointer = subAccount.getPointer();
                initObservables(pointer);
            }
            Log.d("OBS", "setSubaccountData init took " + (System.currentTimeMillis()-millis) +"ms");
            setSubaccountData(subAccounts);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initObservables(int pointer) {
        if (mTransactionDataObservables.get(pointer) == null) {
            final TransactionDataObservable transactionDataObservable = new TransactionDataObservable(mSession,
                                                                                                      mExecutor,
                                                                                                      pointer, false);
            mActiveAccountObservable.addObserver(transactionDataObservable);
            mBlockchainHeightObservable.addObserver(transactionDataObservable);
            mTransactionDataObservables.put(pointer, transactionDataObservable);
        }
        if (mBalanceDataObservables.get(pointer) == null) {
            final BalanceDataObservable balanceDataObservable = new BalanceDataObservable(mSession, mExecutor, pointer);
            mBlockchainHeightObservable.addObserver(balanceDataObservable);
            mBalanceDataObservables.put(pointer, balanceDataObservable);
        }
        if (mReceiveAddressObservables.get(pointer) == null) {
            final ReceiveAddressObservable addressObservable = new ReceiveAddressObservable(mSession, mExecutor, pointer);
            mReceiveAddressObservables.put(pointer, addressObservable);
            mActiveAccountObservable.addObserver(addressObservable);
        }
        if (mUTXODataObservables.get(pointer) == null) {
            final TransactionDataObservable utxoDataObservable = new TransactionDataObservable(mSession, mExecutor,
                                                                                               pointer, true);
            mBlockchainHeightObservable.addObserver(utxoDataObservable);
            mUTXODataObservables.put(pointer, utxoDataObservable);
        }
    }


    public List<SubaccountData> getSubaccountDataList() {
        return mSubaccountData;
    }

    public SubaccountData getSubaccountDataWithPointer(final Integer pointer) {
        for (final SubaccountData subaccountData : mSubaccountData) {
            if (subaccountData.getPointer().equals(pointer))
                return subaccountData;
        }
        return null;
    }

    public void setSubaccountData(final List<SubaccountData> subaccountData) {
        Log.d("OBS", "setSubaccountData(" + subaccountData +")");
        this.mSubaccountData = subaccountData;
        setChanged();
        notifyObservers();
    }

    public void add(SubaccountData newSubAccount) {
        mSubaccountData.add(newSubAccount);
        final Integer newPointer = newSubAccount.getPointer();
        initObservables(newPointer);
        mReceiveAddressObservables.get(newPointer).refresh();
        mBalanceDataObservables.get(newPointer).refresh();
        setSubaccountData(mSubaccountData);
    }
}
