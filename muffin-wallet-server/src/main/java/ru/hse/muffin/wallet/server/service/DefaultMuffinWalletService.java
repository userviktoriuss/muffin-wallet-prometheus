package ru.hse.muffin.wallet.server.service;


import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import ru.hse.muffin.wallet.data.api.MuffinTransactionRepository;
import ru.hse.muffin.wallet.data.api.MuffinWalletRepository;
import ru.hse.muffin.wallet.server.dto.CurrencyRate;
import ru.hse.muffin.wallet.server.dto.MuffinTransaction;
import ru.hse.muffin.wallet.server.dto.MuffinWallet;
import ru.hse.muffin.wallet.server.exception.MuffinWalletNotFoundException;
import ru.hse.muffin.wallet.server.mapper.MuffinWalletMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultMuffinWalletService implements MuffinWalletService {

  private final MuffinWalletMapper muffinWalletMapper;

  private final MuffinWalletRepository muffinWalletRepository;

  private final MuffinTransactionRepository muffinTransactionRepository;

  @Value("${currency.service.url}")
  private String serviceUrl;

  private final RestTemplate restTemplate;

  @Override
  public MuffinWallet getMuffinWallet(UUID id) {
    log.warn("getMuffinWallet");
    return muffinWalletMapper.dataDtoToMuffinWalletServiceDto(
        muffinWalletRepository.findById(id).orElseThrow(MuffinWalletNotFoundException::new));
  }

  @Override
  public Page<MuffinWallet> getMuffinWalletsByOwner(String ownerName, Pageable pageable) {
    log.info("getMuffinWalletsByOwner");
    if (ownerName != null) {
      return getMuffinWalletsByOwnerNameNotNull(ownerName, pageable);
    }

    return getAllMuffinWallets(pageable);
  }

  private Page<MuffinWallet> getMuffinWalletsByOwnerNameNotNull(
      String ownerName, Pageable pageable) {
    log.info("getMuffinWalletsByOwnerNameNotNull");
    return muffinWalletRepository
        .findByOwnerNameLike(ownerName, pageable)
        .map(muffinWalletMapper::dataDtoToMuffinWalletServiceDto);
  }

  private Page<MuffinWallet> getAllMuffinWallets(Pageable pageable) {
    log.info("getAllMuffinWallets");
    return muffinWalletRepository
        .findAll(pageable)
        .map(muffinWalletMapper::dataDtoToMuffinWalletServiceDto);
  }

  @Override
  public MuffinWallet createMuffinWallet(MuffinWallet muffinWallet) {
    log.info("createMuffinWallet");
    return muffinWalletMapper.dataDtoToMuffinWalletServiceDto(
        muffinWalletRepository.save(
            muffinWalletMapper.serviceDtoToMuffinWalletDataDto(muffinWallet)));
  }

  @Override
  @Transactional
  public MuffinTransaction createMuffinTransaction(MuffinTransaction muffinTransaction) {
    log.info("createMuffinTransaction");
    muffinWalletRepository.findByIdInForUpdate(
        List.of(
            muffinTransaction.getFromMuffinWalletId(), muffinTransaction.getToMuffinWalletId()));

    var fromWallet =
        muffinWalletRepository
            .findById(muffinTransaction.getFromMuffinWalletId())
            .orElseThrow(MuffinWalletNotFoundException::new);

    var toWallet =
        muffinWalletRepository
            .findById(muffinTransaction.getToMuffinWalletId())
            .orElseThrow(MuffinWalletNotFoundException::new);

    muffinTransaction.setCurrency(getCurrency(fromWallet.getType(), toWallet.getType()));

    fromWallet.setBalance(fromWallet.getBalance().subtract(muffinTransaction.getAmount()));
    muffinWalletRepository.update(fromWallet);

    toWallet.setBalance(toWallet.getBalance().add(muffinTransaction.getAmount().multiply(muffinTransaction.getCurrency())));
    muffinWalletRepository.update(toWallet);

    return muffinWalletMapper.dataDtoToMuffinTransactionServiceDto(
        muffinTransactionRepository.save(
            muffinWalletMapper.serviceDtoToMuffinTransactionDataDto(muffinTransaction)));
  }

  private BigDecimal getCurrency(String from, String to){
    String url = String.format("%s?from=%s&to=%s", serviceUrl, from, to);
    return restTemplate.getForObject(url, CurrencyRate.class).getRate();
  }
}
