package feveid.department.service.impl;

import feveid.common.exception.ConflictException;
import feveid.common.exception.NotFoundException;
import feveid.department.dto.DepartmentRequest;
import feveid.department.dto.DepartmentResponse;
import feveid.department.entity.Department;
import feveid.department.repository.DepartmentRepository;
import feveid.department.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartments() {
        return departmentRepository.findAllByOrderByNameAsc().stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public DepartmentResponse getDepartmentById(Long id) {
        return DepartmentResponse.from(findById(id));
    }

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByNameIgnoreCase(request.name())){
            throw new ConflictException("Tên ban đã tồn tại");
        }
        Department saved = departmentRepository.save(Department.builder()
                .name(request.name())
                .description(request.description())
                .build());
        return DepartmentResponse.from(saved);
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = findById(id);
        // So sánh các ban KHÁC (IdNot): giữ nguyên tên của chính mình thì không phải trùng
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)){
            throw new ConflictException("Tên ban đã tồn tại");
        }
        department.setName(request.name());
        department.setDescription(request.description());

        // Dirty checking: không cần save()
        return DepartmentResponse.from(department);
    }
    private Department findById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ban"));
    }
}
