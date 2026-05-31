package br.com.poderfinanceiro.app.util;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

/**
 * Sobrescreve o mapeamento de NAMED_ENUM para VARCHAR no H2.
 * Necessário porque @JdbcTypeCode(SqlTypes.NAMED_ENUM) tem precedência
 * sobre as propriedades globais do Hibernate.
 */
public class H2EnumTypeContributor implements TypeContributor {

    @Override
    public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        typeContributions.getTypeConfiguration()
                .getJdbcTypeRegistry()
                .addDescriptor(SqlTypes.NAMED_ENUM, VarcharJdbcType.INSTANCE);
    }
}
