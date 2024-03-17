package com.example.vinhomeproject.service;

import com.example.vinhomeproject.dto.RevenuePerMonthDTO;
import com.example.vinhomeproject.dto.RevenueYearDTO;
import com.example.vinhomeproject.models.Contract;
import com.example.vinhomeproject.models.ContractHistory;
import com.example.vinhomeproject.models.Payment;
import com.example.vinhomeproject.repositories.ContractRepository;
import com.example.vinhomeproject.repositories.PaymentRepository;
import com.example.vinhomeproject.response.ResponseObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {
    @Autowired
    private  PaymentRepository rs;
    @Autowired
    private ContractRepository contractRepository;

    public ResponseEntity<ResponseObject> getAllPayment(){
        List<Payment> paymentList=rs.findAll();

        return ResponseEntity.ok(new ResponseObject(
                "successfully",
                paymentList
        ));
    }
    public ResponseEntity<ResponseObject> getPaymentById(Long id){
        return ResponseEntity.ok(new ResponseObject(
                "successfully",
                rs.findPaymentById(id)
        ));

    }


    public  ResponseEntity<String> deletePayment(Long id) {
        Payment existingUser = rs.findPaymentById(id);

        if (existingUser != null) {
            existingUser.setStatus(!existingUser.isStatus());
            rs.save(existingUser);
            return ResponseEntity.ok("delete successfully");
        }else {
            return ResponseEntity.ok("id not exist");
        }


    }

    public ResponseEntity<String> updatePayment(Long id,Payment paypent) {
        Payment existingUser = rs.findPaymentById(id);

        if (existingUser != null) {
            if (paypent.getContent()!=null){ existingUser.setContent(paypent.getContent());}
            if (paypent.getPayment_time()!=null){existingUser.setPayment_time(paypent.getPayment_time());}
            if (paypent.getPaymentType()!=null){ existingUser.setPaymentType(paypent.getPaymentType());}
            if (paypent.getTotal_price()!=0){existingUser.setTotal_price(paypent.getTotal_price());}
             rs.save(existingUser);
             return ResponseEntity.ok("update successfully");
        }else {
            return ResponseEntity.ok("id not exist");
        }
    }
    public ResponseEntity<String> createPayment(Payment id) {
        Payment existingUser = new Payment();
        existingUser.setContent(id.getContent());
        existingUser.setPayment_time(id.getPayment_time());
        existingUser.setPaymentType(id.getPaymentType());
        existingUser.setTotal_price(id.getTotal_price());
        existingUser.setStatus(true);
        rs.save(existingUser);
        return ResponseEntity.ok("create successfully");
    }
    public ResponseEntity<ResponseObject> compareRevenue(int year){
        return ResponseEntity.ok(new ResponseObject(
                "",
                RevenueYearDTO.builder()
                        .compareYear(mapToRevenuePerMonthDTO(calculateRevenueByYear(year)))
                        .currentYear(mapToRevenuePerMonthDTO(calculateRevenueByYear(LocalDate.now().getYear())))
                        .build()
        ));
    }
    public ResponseEntity<ResponseObject> calculateRevenueByMonth(int year, int month) {
        Map<Integer, Double> revenueMap = new HashMap<>();
        List<Payment> paymentsOfMonth = rs.findByPaymentTimeBetween(
                LocalDate.of(year, month, 1),
                LocalDate.of(year, month, 31)
        );

        double totalRevenueOfMonth = paymentsOfMonth.stream()
                .mapToDouble(Payment::getTotal_price)
                .sum();
        return ResponseEntity.ok(new ResponseObject("",totalRevenueOfMonth));
    }
    public void CreatePayment(Long contractId, int paymentOrder){
        if(contractRepository.findById(contractId).isEmpty()){
            return;
        }
        Contract contract = contractRepository.findById(contractId).get();
        Payment payment = Payment.builder()
                .payment_time(contract.getDateStartRent().plusMonths(paymentOrder))
                .contract(contract)
                .total_price(contract.getContractHistory().getPrice())
                .content("Semester "+String.valueOf(paymentOrder))
                .build();
        payment.setStatus(false);
        rs.save(payment);
    }
    private Map<Integer, Double> calculateRevenueByYear(int year) {
        Map<Integer, Double> revenueMap = new HashMap<>();
        List<Payment> paymentsOfYear = rs.findByPaymentTimeBetween(
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31)
        );
        paymentsOfYear.removeIf(payment -> !payment.isStatus());
        for (int month = 1; month <= 12; month++) {
            final int currentMonth = month;
            double totalRevenueOfMonth = paymentsOfYear.stream()
                    .filter(payment -> payment.getPayment_time().getMonthValue() == currentMonth)
                    .mapToDouble(Payment::getTotal_price)
                    .sum();
            revenueMap.put(month, totalRevenueOfMonth);
        }
        return revenueMap;
    }
    private List<RevenuePerMonthDTO> mapToRevenuePerMonthDTO(Map<Integer, Double> revenueMap) {
        List<RevenuePerMonthDTO> revenueList = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : revenueMap.entrySet()) {
            int month = entry.getKey();
            double revenue = entry.getValue();
            revenueList.add(new RevenuePerMonthDTO(month, revenue));
        }
        return revenueList;
    }
}
